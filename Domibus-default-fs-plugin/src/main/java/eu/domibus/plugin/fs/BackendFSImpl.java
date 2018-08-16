package eu.domibus.plugin.fs;

import eu.domibus.common.*;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.AbstractBackendConnector;
import eu.domibus.plugin.fs.ebms3.CollaborationInfo;
import eu.domibus.plugin.fs.ebms3.Property;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.worker.FSSendMessagesService;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.activation.DataHandler;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static eu.domibus.common.MessageStatus.*;

/**
 * File system backend integration plugin.
 *
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public class BackendFSImpl extends AbstractBackendConnector<FSMessage, FSMessage> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(BackendFSImpl.class);


    private static final String FILENAME_SANITIZE_REGEX = "[^\\w.-]";
    private  static final String FILENAME_SANITIZE_REPLACEMENT = "_";

    private static final Set<MessageStatus> SENDING_MESSAGE_STATUSES = EnumSet.of(
            READY_TO_SEND, SEND_ENQUEUED, SEND_IN_PROGRESS, WAITING_FOR_RECEIPT,
            WAITING_FOR_RETRY, SEND_ATTEMPT_FAILED
    );

    private static final Set<MessageStatus> SEND_SUCCESS_MESSAGE_STATUSES = EnumSet.of(
            ACKNOWLEDGED, ACKNOWLEDGED_WITH_WARNING
    );

    private static final Set<MessageStatus> SEND_FAILED_MESSAGE_STATUSES = EnumSet.of(
            SEND_FAILURE
    );



    // receiving statuses should be REJECTED, RECEIVED_WITH_WARNINGS, DOWNLOADED, DELETED, RECEIVED

    @Autowired
    private FSMessageTransformer defaultTransformer;
    
    @Autowired
    private FSFilesManager fsFilesManager;
    
    @Autowired
    private FSPluginProperties fsPluginProperties;

    @Autowired
    private DomibusConfigurationExtService domibusConfigurationExtService;

    @Autowired
    private DomainContextExtService domainContextExtService;

    @Autowired
    private FSSendMessagesService fsSendMessagesService;

    private final Map<String, Pattern> domainPatternCache = new HashMap<>();

    /**
     * Creates a new <code>BackendFSImpl</code>.
     *
     * @param name Connector name
     */
    public BackendFSImpl(String name) {
        super(name);
    }

    /**
     * The implementations of the transformer classes are responsible for
     * transformation between the native backend formats and
     * eu.domibus.plugin.Submission.
     *
     * @return MessageSubmissionTransformer
     */
    @Override
    public MessageSubmissionTransformer<FSMessage> getMessageSubmissionTransformer() {
        return this.defaultTransformer;
    }

    /**
     * The implementations of the transformer classes are responsible for
     * transformation between the native backend formats and
     * eu.domibus.plugin.Submission.
     *
     * @return MessageRetrievalTransformer
     */
    @Override
    public MessageRetrievalTransformer<FSMessage> getMessageRetrievalTransformer() {
        return this.defaultTransformer;
    }

    @Override
    public void deliverMessage(String messageId) {
        LOG.debug("Delivering File System Message [{}]", messageId);
        FSMessage fsMessage;

        // Download message
        try {
            fsMessage = downloadMessage(messageId, null);
        } catch (MessageNotFoundException e) {
            throw new FSPluginException("Unable to download message " + messageId, e);
        }

        //extract final recipient
        final String finalRecipient = getFinalRecipient(fsMessage.getMetadata());
        if (StringUtils.isBlank(finalRecipient)) {
            throw new FSPluginException("Unable to extract finalRecipient from message " + messageId);
        }
        final String finalRecipientFolder = sanitizeFileName(finalRecipient);

        // get multiTenantAware
        boolean multiTenantAware = domibusConfigurationExtService.isMultiTenantAware();

        // get domain info
        String domain;
        if(multiTenantAware) {
            domain = domainContextExtService.getCurrentDomain().getCode();
        } else {
            domain = resolveDomain(fsMessage);
        }

        // Persist message
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
             FileObject incomingFolder = fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
             FileObject incomingFolderByRecipient = fsFilesManager.getEnsureChildFolder(incomingFolder, finalRecipientFolder);
             FileObject incomingFolderByMessageId = fsFilesManager.getEnsureChildFolder(incomingFolderByRecipient, messageId)) {

            //let's write the metadata file first
            try (FileObject fileObject = incomingFolderByMessageId.resolveFile(FSSendMessagesService.METADATA_FILE_NAME);
                 FileContent fileContent = fileObject.getContent()) {

                writeMetadata(fileContent.getOutputStream(), fsMessage.getMetadata());
                LOG.info("Message metadata file written at: [{}]", fileObject.getName().getURI());
            }

            //write payloads
            for (Map.Entry<String, FSPayload> entry : fsMessage.getPayloads().entrySet()) {
                FSPayload fsPayload = entry.getValue();
                DataHandler dataHandler = fsPayload.getDataHandler();
                String contentId = entry.getKey();
                String fileName = getFileName(contentId, fsPayload);

                try (FileObject fileObject = incomingFolderByMessageId.resolveFile(fileName);
                     FileContent fileContent = fileObject.getContent()) {
                    dataHandler.writeTo(fileContent.getOutputStream());
                    LOG.info("Message payload received: [{}]", fileObject.getName());
                }
            }
        } catch (JAXBException ex) {
            throw new FSPluginException("An error occurred while writing metadata for downloaded message " + messageId, ex);
        } catch (IOException | FSSetUpException ex) {
            throw new FSPluginException("An error occurred persisting downloaded message " + messageId, ex);
        }
    }

    private String getFileName(String contentId, FSPayload fsPayload) {
        //original name + extension
        String fileName = fsPayload.getFileName();

        //if empty, we compose it - based on cid and extension
        if (StringUtils.isBlank(fileName)) {
            fileName = contentId.replaceFirst("cid:", StringUtils.EMPTY) + getFileNameExtension(fsPayload.getMimeType());
        }
        return fileName;
    }

    private String getFileNameExtension(String mimeType) {
        String extension = StringUtils.EMPTY;
        try {
            extension = FSMimeTypeHelper.getExtension(mimeType);
        } catch (MimeTypeException ex) {
            LOG.warn("Error parsing MIME type", ex);
        }
        return extension;
    }

    private String resolveDomain(FSMessage fsMessage) {
        CollaborationInfo collaborationInfo = fsMessage.getMetadata().getCollaborationInfo();
        String service = collaborationInfo.getService().getValue();
        String action = collaborationInfo.getAction();
        return resolveDomain(service, action);
    }

    String resolveDomain(String service, String action) {
        String serviceAction = service + "#" + action;
        List<String> domains = fsPluginProperties.getDomains();
        for (String domain : domains) {
            Pattern domainExpressionPattern = getDomainPattern(domain);
            if (domainExpressionPattern != null) {
                boolean domainMatches = domainExpressionPattern.matcher(serviceAction).matches();
                if (domainMatches) {
                    return domain;
                }
            }
        }
        return null;
    }
    
    private Pattern getDomainPattern(String domain) {
        if (domainPatternCache.containsKey(domain)) {
            return domainPatternCache.get(domain);
        } else {
            String domainExpression = fsPluginProperties.getExpression(domain);
            Pattern domainExpressionPattern = null;
            if (StringUtils.isNotEmpty(domainExpression)) {
                try {
                    domainExpressionPattern = Pattern.compile(domainExpression);
                } catch (PatternSyntaxException e) {
                    LOG.warn("Invalid domain expression for " + domain, e);
                }
            }

            // domainExpressionPattern may be null, we should still cache null and return it
            domainPatternCache.put(domain, domainExpressionPattern);
            return domainExpressionPattern;
        }
    }

    @Override
    public void messageReceiveFailed(MessageReceiveFailureEvent messageReceiveFailureEvent) {
        // No-op
        // Probably, the AbstractBackendConnector should not throw the UnsupportedOperationException
    }

    @Override
    public void messageSendSuccess(String messageId) {
        // Implemented in messageStatusChanged to avoid event collision and use improved API
        // Probably, the AbstractBackendConnector should not throw the UnsupportedOperationException
    }

    @Override
    public void messageSendFailed(String messageId) {
        // Implemented in messageStatusChanged to avoid event collision and use improved API
        // Probably, the AbstractBackendConnector should implement a default no-op
    }

    private void handleSendFailedMessage(String domain, String messageId) {
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
             FileObject outgoingFolder = fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
             FileObject targetFileMessage = findMessageFile(outgoingFolder, messageId)) {

            fsSendMessagesService.handleSendFailedMessage(targetFileMessage, domain, getErrorMessage(messageId));

        } catch (IOException e){
            throw new FSPluginException("Error handling the send failed message file " + messageId, e);
        }
    }


    private String getErrorMessage(String messageId) throws IOException {
        List<ErrorResult> errors = super.getErrorsForMessage(messageId);
        String content;
        if (!errors.isEmpty()) {
            ErrorResult lastError = errors.get(errors.size() - 1);
            content = String.valueOf(getErrorFileContent(lastError));
        } else {
            // This might occur when the destination host is unreachable
            content = "Error detail information is not available";
            LOG.error("[{}] for [{}]", content, messageId);
        }
        return content;
    }


    private StringBuilder getErrorFileContent(ErrorResult errorResult) {

        return fsSendMessagesService.buildErrorMessage(errorResult.getErrorCode().getErrorCodeName(),
                                                        errorResult.getErrorDetail(),
                                                        errorResult.getMessageInErrorId(),
                                                        errorResult.getMshRole().toString(),
                                                        errorResult.getNotified().toString(),
                                                        errorResult.getTimestamp().toString());
    }

    private void handleSentMessage(String domain, String messageId) {
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
             FileObject outgoingFolder = fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
             FileObject targetFileMessage = findMessageFile(outgoingFolder, messageId)) {

            if (targetFileMessage != null) {
                if (fsPluginProperties.isSentActionDelete(domain)) {
                    //Delete
                    fsFilesManager.deleteFile(targetFileMessage);
                    LOG.debug("Successfully sent message file [{}] was deleted", messageId);
                } else if (fsPluginProperties.isSentActionArchive(domain)) {
                    // Archive
                    String targetFileMessageURI = targetFileMessage.getParent().getName().getPath();
                    String sentDirectoryLocation = FSFileNameHelper.deriveSentDirectoryLocation(targetFileMessageURI);
                    FileObject sentDirectory = fsFilesManager.getEnsureChildFolder(rootDir, sentDirectoryLocation);

                    String baseName = targetFileMessage.getName().getBaseName();
                    String newName = FSFileNameHelper.stripStatusSuffix(baseName);
                    FileObject archivedFile = sentDirectory.resolveFile(newName);
                    fsFilesManager.moveFile(targetFileMessage, archivedFile);

                    LOG.debug("Successfully sent message file [{}] was archived into [{}]", messageId, archivedFile.getName().getURI());
                }
            } else {
                LOG.error("The successfully sent message file [{}] was not found in domain [{}]", messageId, domain);
            }
        } catch (FileSystemException e) {
            LOG.error("Error handling the successfully sent message file [" + messageId + "]", e);
        }
    }

    private FileObject findMessageFile(FileObject parentDir, String messageId) throws FileSystemException {
        FileObject[] files = fsFilesManager.findAllDescendantFiles(parentDir);
        try {
            FileObject targetFile = null;
            for (FileObject file : files) {
                String baseName = file.getName().getBaseName();
                if (FSFileNameHelper.isMessageRelated(baseName, messageId)) {
                    targetFile = file;
                    break;
                }
            }
            return targetFile;
        } finally {
            fsFilesManager.closeAll(files);
        }
    }

    @Override
    public void messageStatusChanged(MessageStatusChangeEvent event) {
        String messageId = event.getMessageId();

        // get multiTenantAware
        boolean multiTenantAware = domibusConfigurationExtService.isMultiTenantAware();

        // get domain info
        String domain;
        if(multiTenantAware) {
            domain = domainContextExtService.getCurrentDomain().getCode();
        } else {

            Map<String, Object> properties = event.getProperties();
            String service = (String) properties.get("service");
            String action = (String) properties.get("action");

            domain = resolveDomain(service, action);
        }

        LOG.debug("Message [{}] changed status from [{}] to [{}] in domain [{}]",
                messageId, event.getFromStatus(), event.getToStatus(), domain);

        if (isSendingEvent(event)) {
            renameMessageFile(domain, messageId, event.getToStatus());
        } else if (isSendSuccessEvent(event)) {
            handleSentMessage(domain, messageId);
        } else if (isSendFailedEvent(event)) {
            handleSendFailedMessage(domain, messageId);
        }
    }

    private boolean isSendingEvent(MessageStatusChangeEvent event) {
        return SENDING_MESSAGE_STATUSES.contains(event.getToStatus());
    }

    private boolean isSendSuccessEvent(MessageStatusChangeEvent event) {
        return SEND_SUCCESS_MESSAGE_STATUSES.contains(event.getToStatus());
    }

    private boolean isSendFailedEvent(MessageStatusChangeEvent event) {
        return SEND_FAILED_MESSAGE_STATUSES.contains(event.getToStatus());
    }

    private void renameMessageFile(String domain, String messageId, MessageStatus status) {
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
                FileObject outgoingFolder = fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
                FileObject targetFile = findMessageFile(outgoingFolder, messageId)) {
            
            if (targetFile != null) {
                String baseName = targetFile.getName().getBaseName();
                String newName = FSFileNameHelper.deriveFileName(baseName, status);
                fsFilesManager.renameFile(targetFile, newName);
            } else {
                LOG.error("The message to rename [{}] was not found in domain [{}]", messageId, domain);
            }
        } catch (FileSystemException ex) {
            LOG.error("Error renaming file", ex);
        } catch (FSSetUpException ex) {
            LOG.error("Error setting up folders for domain [" + domain + "]", ex);
        }
    }

    /**
     * extracts finalRecipient from message properties
     *
     * @see UserMessage
     * @param userMessage Object which contains finalRecipient info
     * @return finalRecipient String
     */
    protected String getFinalRecipient(final UserMessage userMessage) {
        String finalRecipient = null;
        for (final Property p : userMessage.getMessageProperties().getProperty()) {
            if (p.getName() != null && p.getName().equals(MessageConstants.FINAL_RECIPIENT)) {
                finalRecipient = p.getValue();
                break;
            }
        }
        return finalRecipient;
    }

    /**
     * replacing all non [a-zA-z0-9] characters with _ from a fileName
     *
     * @param fileName filename to be sanitized
     * @return sanitized fileName
     */
    protected String sanitizeFileName(@NotNull final String fileName) {
        return fileName.replaceAll(FILENAME_SANITIZE_REGEX, FILENAME_SANITIZE_REPLACEMENT);
    }

    /**
     * Writes metadata file
     *
     * @param outputStream {@link OutputStream} to write the xml
     * @param userMessage  Object which contains metadata to be printed
     * @throws JAXBException exception thrown
     */
    private void writeMetadata(OutputStream outputStream, UserMessage userMessage) throws JAXBException {
        FSXMLHelper.writeXML(outputStream, UserMessage.class, userMessage);
    }

}
