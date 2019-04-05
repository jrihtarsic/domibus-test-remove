package eu.domibus.plugin.handler;

import com.google.common.collect.Sets;
import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.*;
import eu.domibus.common.dao.*;
import eu.domibus.common.exception.CompressionException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.exception.MessagingExceptionFactory;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.logging.ErrorLogEntry;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.common.services.MessagingService;
import eu.domibus.common.services.impl.MessageIdGenerator;
import eu.domibus.common.validators.BackendMessageValidator;
import eu.domibus.common.validators.PayloadProfileValidator;
import eu.domibus.common.validators.PropertyProfileValidator;
import eu.domibus.configuration.storage.StorageProvider;
import eu.domibus.core.message.fragment.SplitAndJoinService;
import eu.domibus.core.pmode.PModeDefaultService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.core.pull.PartyExtractor;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.ebms3.common.context.MessageExchangeConfiguration;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.*;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.transformer.impl.SubmissionAS4Transformer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible of handling the plugins requests for all the operations exposed.
 * During submit, it manages the user authentication and the AS4 message's validation, compression and saving.
 * During download, it manages the user authentication and the AS4 message's reading, data clearing and status update.
 *
 * @author Christian Koch, Stefan Mueller, Federico Martini, Ioana Dragusanu
 * @author Cosmin Baciu
 * @since 3.0
 */
@Service
public class DatabaseMessageHandler implements MessageSubmitter, MessageRetriever, MessagePuller {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DatabaseMessageHandler.class);
    private static final String MESSAGE_WITH_ID_STR = "Message with id [";
    private static final String WAS_NOT_FOUND_STR = "] was not found";
    private static final String ERROR_SUBMITTING_THE_MESSAGE_STR = "Error submitting the message [";
    private static final String TO_STR = "] to [";

    private final ObjectFactory ebMS3Of = new ObjectFactory();


    @Autowired
    private SubmissionAS4Transformer transformer;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private SignalMessageDao signalMessageDao;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private UserMessageLogService userMessageLogService;

    @Autowired
    private StorageProvider storageProvider;

    @Autowired
    private SignalMessageLogDao signalMessageLogDao;

    @Autowired
    private ErrorLogDao errorLogDao;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private MessageIdGenerator messageIdGenerator;

    @Autowired
    private PayloadProfileValidator payloadProfileValidator;

    @Autowired
    private PropertyProfileValidator propertyProfileValidator;

    @Autowired
    private BackendMessageValidator backendMessageValidator;

    @Autowired
    private MessageExchangeService messageExchangeService;

    @Autowired
    private PullMessageService pullMessageService;

    @Autowired
    protected AuthUtils authUtils;

    @Autowired
    protected UserMessageService userMessageService;

    @Autowired
    protected UIReplicationSignalService uiReplicationSignalService;

    @Autowired
    protected SplitAndJoinService splitAndJoinService;

    @Autowired
    protected PModeDefaultService pModeDefaultService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Submission downloadMessage(final String messageId) throws MessageNotFoundException {
        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }
        LOG.info("Downloading message with id [{}]", messageId);
        String originalUser = authUtils.getOriginalUserFromSecurityContext();
        String displayUser = originalUser == null ? "super user" : originalUser;
        LOG.debug("Authorized as [{}]", displayUser);

        UserMessage userMessage;
        try {
            LOG.debug("Searching message with id [{}]", messageId);
            userMessage = messagingDao.findUserMessageByMessageId(messageId);
            // Authorization check
            validateOriginalUser(userMessage, originalUser, MessageConstants.FINAL_RECIPIENT);

            UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId, MSHRole.RECEIVING);
            if (userMessageLog == null) {
                throw new MessageNotFoundException(MESSAGE_WITH_ID_STR + messageId + WAS_NOT_FOUND_STR);
            }
        } catch (final NoResultException nrEx) {
            LOG.debug(MESSAGE_WITH_ID_STR + messageId + WAS_NOT_FOUND_STR, nrEx);
            throw new MessageNotFoundException(MESSAGE_WITH_ID_STR + messageId + WAS_NOT_FOUND_STR);
        }

        userMessageLogService.setMessageAsDownloaded(messageId);
        // Deleting the message and signal message if the retention download is zero and the payload is not stored on the file system.
        if (userMessage != null && 0 == pModeProvider.getRetentionDownloadedByMpcURI(userMessage.getMpc()) && !userMessage.isPayloadOnFileSystem()) {
            messagingDao.clearPayloadData(messageId);
            List<SignalMessage> signalMessages = signalMessageDao.findSignalMessagesByRefMessageId(messageId);
            if (!signalMessages.isEmpty()) {
                for (SignalMessage signalMessage : signalMessages) {
                    signalMessageDao.clear(signalMessage);
                }
            }
            // Sets the message log status to DELETED
            userMessageLogService.setMessageAsDeleted(messageId);
            // Sets the log status to deleted also for the signal messages (if present).
            List<String> signalMessageIds = signalMessageDao.findSignalMessageIdsByRefMessageId(messageId);
            if (!signalMessageIds.isEmpty()) {
                for (String signalMessageId : signalMessageIds) {
                    userMessageLogService.setMessageAsDeleted(signalMessageId);
                    LOG.info("SignalMessage [{}] was set as DELETED.", signalMessageId);
                }
            }
        }
        return transformer.transformFromMessaging(userMessage);
    }

    protected void validateOriginalUser(UserMessage userMessage, String authOriginalUser, List<String> recipients) {
        if (authOriginalUser != null) {
            LOG.debug("OriginalUser is [{}]", authOriginalUser);
            /* check the message belongs to the authenticated user */
            boolean found = false;
            for (String recipient : recipients) {
                String originalUser = getOriginalUser(userMessage, recipient);
                if (originalUser != null && originalUser.equalsIgnoreCase(authOriginalUser)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                LOG.debug("User [{}] is trying to submit/access a message having as final recipients: [{}]", authOriginalUser, recipients);
                throw new AccessDeniedException("You are not allowed to handle this message. You are authorized as [" + authOriginalUser + "]");
            }
        }
    }

    protected void validateOriginalUser(UserMessage userMessage, String authOriginalUser, String recipient) {
        if (authOriginalUser != null) {
            LOG.debug("OriginalUser is [{}]", authOriginalUser);
            /* check the message belongs to the authenticated user */
            String originalUser = getOriginalUser(userMessage, recipient);
            if (originalUser != null && !originalUser.equalsIgnoreCase(authOriginalUser)) {
                LOG.debug("User [{}] is trying to submit/access a message having as final recipient: [{}]", authOriginalUser, originalUser);
                throw new AccessDeniedException("You are not allowed to handle this message. You are authorized as [" + authOriginalUser + "]");
            }
        }
    }

    private String getOriginalUser(UserMessage userMessage, String type) {
        if (userMessage == null || userMessage.getMessageProperties() == null || userMessage.getMessageProperties().getProperty() == null) {
            return null;
        }
        String originalUser = null;
        for (Property property : userMessage.getMessageProperties().getProperty()) {
            if (property.getName() != null && property.getName().equalsIgnoreCase(type)) {
                originalUser = property.getValue();
                break;
            }
        }
        return originalUser;
    }

    protected void validateAccessToStatusAndErrors(String messageId) {
        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }

        // check if user can get the status/errors of that message (only admin or original users are authorized to do that)
        UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        String originalUser = authUtils.getOriginalUserFromSecurityContext();
        List<String> recipients = new ArrayList<>();
        recipients.add(MessageConstants.ORIGINAL_SENDER);
        recipients.add(MessageConstants.FINAL_RECIPIENT);
        validateOriginalUser(userMessage, originalUser, recipients);
    }

    @Override
    public MessageStatus getStatus(final String messageId) {
        validateAccessToStatusAndErrors(messageId);
        return userMessageLogDao.getMessageStatus(messageId);
    }

    @Override
    public List<? extends ErrorResult> getErrorsForMessage(final String messageId) {
        validateAccessToStatusAndErrors(messageId);
        return errorLogDao.getErrorsForMessage(messageId);
    }

    //TODO refactor this method in order to reuse existing code from the method submit
    @Transactional
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public String submitMessageFragment(UserMessage userMessage, String backendName) throws MessagingProcessingException {
        if (userMessage == null) {
            LOG.warn("UserMessage is null");
            throw new MessageNotFoundException("UserMessage is null");
        }

        // MessageInfo is always initialized in the get method
        MessageInfo messageInfo = userMessage.getMessageInfo();
        String messageId = messageInfo.getMessageId();

        if (StringUtils.isEmpty(messageId)) {
            throw new MessagingProcessingException("Message fragment id is empty");
        }
        LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);
        LOG.debug("Preparing to submit message fragment");

        try {
            // handle if the messageId is unique.
            if (!MessageStatus.NOT_FOUND.equals(userMessageLogDao.getMessageStatus(messageId))) {
                throw new DuplicateMessageException(MESSAGE_WITH_ID_STR + messageId + "] already exists. Message identifiers must be unique");
            }

            Messaging message = ebMS3Of.createMessaging();
            message.setUserMessage(userMessage);

            MessageExchangeConfiguration userMessageExchangeConfiguration = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING);
            String pModeKey = userMessageExchangeConfiguration.getPmodeKey();


            Party to = messageValidations(userMessage, pModeKey, backendName);

            LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);

            fillMpc(userMessage, legConfiguration, to);

            try {
                messagingService.storeMessage(message, MSHRole.SENDING, legConfiguration, backendName);
            } catch (CompressionException exc) {
                LOG.businessError(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_COMPRESSION_FAILURE, userMessage.getMessageInfo().getMessageId());
                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0303, exc.getMessage(), userMessage.getMessageInfo().getMessageId(), exc);
                ex.setMshRole(MSHRole.SENDING);
                throw ex;
            }
            MessageStatus messageStatus = messageExchangeService.getMessageStatus(userMessageExchangeConfiguration);
            userMessageLogService.save(messageId, messageStatus.toString(), pModeDefaultService.getNotificationStatus(legConfiguration).toString(),
                    MSHRole.SENDING.toString(), getMaxAttempts(legConfiguration), message.getUserMessage().getMpc(),
                    backendName, to.getEndpoint(), userMessage.getCollaborationInfo().getService().getValue(), userMessage.getCollaborationInfo().getAction(), null, true);
            if (MessageStatus.READY_TO_PULL != messageStatus) {
                // Sends message to the proper queue if not a message to be pulled.
                userMessageService.scheduleSending(messageId);
            } else {
                final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
                LOG.debug("[submit]:Message:[{}] add lock", userMessageLog.getMessageId());
                pullMessageService.addPullMessageLock(new PartyExtractor(to), pModeKey, userMessageLog);
            }


            uiReplicationSignalService.userMessageSubmitted(userMessage.getMessageInfo().getMessageId());

            LOG.info("Message fragment submitted");
            return userMessage.getMessageInfo().getMessageId();

        } catch (EbMS3Exception ebms3Ex) {
            LOG.error("Error submitting the message [" + userMessage.getMessageInfo().getMessageId() + "] to [" + backendName + "]", ebms3Ex);
            errorLogDao.create(new ErrorLogEntry(ebms3Ex));
            throw MessagingExceptionFactory.transform(ebms3Ex);
        } catch (PModeException p) {
            LOG.error("Error submitting the message [" + userMessage.getMessageInfo().getMessageId() + "] to [" + backendName + "]" + p.getMessage());
            errorLogDao.create(new ErrorLogEntry(MSHRole.SENDING, userMessage.getMessageInfo().getMessageId(), ErrorCode.EBMS_0010, p.getMessage()));
            throw new PModeMismatchException(p.getMessage(), p);
        }
    }


    @Override
    @Transactional
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public String submit(final Submission messageData, final String backendName) throws MessagingProcessingException {

        if (StringUtils.isNotEmpty(messageData.getMessageId())) {
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageData.getMessageId());
        }
        LOG.debug("Preparing to submit message");
        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }

        String originalUser = authUtils.getOriginalUserFromSecurityContext();
        String displayUser = originalUser == null ? "super user" : originalUser;
        LOG.debug("Authorized as [{}]", displayUser);

        UserMessage userMessage = transformer.transformFromSubmission(messageData);

        if (userMessage == null) {
            LOG.warn("UserMessage is null");
            throw new MessageNotFoundException("UserMessage is null");
        }

        validateOriginalUser(userMessage, originalUser, MessageConstants.ORIGINAL_SENDER);

        try {
            // MessageInfo is always initialized in the get method
            MessageInfo messageInfo = userMessage.getMessageInfo();
            String messageId = messageInfo.getMessageId();
            if (messageId == null) {
                messageId = messageIdGenerator.generateMessageId();
                messageInfo.setMessageId(messageId);
            } else {
                backendMessageValidator.validateMessageId(messageId);
                userMessage.getMessageInfo().setMessageId(messageId);
            }
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageInfo.getMessageId());

            String refToMessageId = messageInfo.getRefToMessageId();
            if (refToMessageId != null) {
                backendMessageValidator.validateRefToMessageId(refToMessageId);
            }
            // handle if the messageId is unique. This should only fail if the ID is set from the outside
            if (!MessageStatus.NOT_FOUND.equals(userMessageLogDao.getMessageStatus(messageId))) {
                throw new DuplicateMessageException(MESSAGE_WITH_ID_STR + messageId + "] already exists. Message identifiers must be unique");
            }

            Messaging message = ebMS3Of.createMessaging();
            message.setUserMessage(userMessage);

            MessageExchangeConfiguration userMessageExchangeConfiguration;

            Party to = null;
            MessageStatus messageStatus = null;
            if (messageExchangeService.forcePullOnMpc(userMessage.getMpc())) {
                // UserMesages submited with the optional mpc attribute are
                // meant for pulling (if the configuration property is enabled)
                userMessageExchangeConfiguration = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, true);
                to = createNewParty(userMessage.getMpc());
                messageStatus = MessageStatus.READY_TO_PULL;
            } else {
                userMessageExchangeConfiguration = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING);
            }
            String pModeKey = userMessageExchangeConfiguration.getPmodeKey();

            if (to == null) {
                to = messageValidations(userMessage, pModeKey, backendName);
            }

            LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
            if (userMessage.getMpc() == null) {
                fillMpc(userMessage, legConfiguration, to);
            }

            payloadProfileValidator.validate(message, pModeKey);
            propertyProfileValidator.validate(message, pModeKey);

            final boolean splitAndJoin = splitAndJoinService.mayUseSplitAndJoin(legConfiguration);
            userMessage.setSplitAndJoin(splitAndJoin);

            if (splitAndJoin && storageProvider.idPayloadsPersistenceInDatabaseConfigured()) {
                LOG.error("SplitAndJoin feature needs payload storage on the file system");
                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0002, "SplitAndJoin feature needs payload storage on the file system", userMessage.getMessageInfo().getMessageId(), null);
                ex.setMshRole(MSHRole.SENDING);
                throw ex;
            }

            try {
                messagingService.storeMessage(message, MSHRole.SENDING, legConfiguration, backendName);
            } catch (CompressionException exc) {
                LOG.businessError(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_COMPRESSION_FAILURE, userMessage.getMessageInfo().getMessageId());
                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0303, exc.getMessage(), userMessage.getMessageInfo().getMessageId(), exc);
                ex.setMshRole(MSHRole.SENDING);
                throw ex;
            }
            if (messageStatus == null) {
                messageStatus = messageExchangeService.getMessageStatus(userMessageExchangeConfiguration);
            }
            final boolean sourceMessage = userMessage.isSourceMessage();
            userMessageLogService.save(messageId, messageStatus.toString(), pModeDefaultService.getNotificationStatus(legConfiguration).toString(),
                    MSHRole.SENDING.toString(), getMaxAttempts(legConfiguration), message.getUserMessage().getMpc(),
                    backendName, to.getEndpoint(), messageData.getService(), messageData.getAction(), sourceMessage, null);
            if (MessageStatus.READY_TO_PULL != messageStatus) {
                // Sends message to the proper queue if not a message to be pulled.
                userMessageService.scheduleSending(messageId);
            } else {
                final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
                LOG.debug("[submit]:Message:[{}] add lock", userMessageLog.getMessageId());
                pullMessageService.addPullMessageLock(new PartyExtractor(to), pModeKey, userMessageLog);
            }


            uiReplicationSignalService.userMessageSubmitted(userMessage.getMessageInfo().getMessageId());

            LOG.info("Message submitted");
            return userMessage.getMessageInfo().getMessageId();

        } catch (EbMS3Exception ebms3Ex) {
            LOG.error(ERROR_SUBMITTING_THE_MESSAGE_STR + userMessage.getMessageInfo().getMessageId() + TO_STR + backendName + "]", ebms3Ex);
            errorLogDao.create(new ErrorLogEntry(ebms3Ex));
            throw MessagingExceptionFactory.transform(ebms3Ex);
        } catch (PModeException p) {
            LOG.error(ERROR_SUBMITTING_THE_MESSAGE_STR + userMessage.getMessageInfo().getMessageId() + TO_STR + backendName + "]" + p.getMessage());
            errorLogDao.create(new ErrorLogEntry(MSHRole.SENDING, userMessage.getMessageInfo().getMessageId(), ErrorCode.EBMS_0010, p.getMessage()));
            throw new PModeMismatchException(p.getMessage(), p);
        }
    }

    @Override
    @Transactional
    public void initiatePull(String mpc) {
        messageExchangeService.initiatePullRequest(mpc);
    }

    private Party messageValidations(UserMessage userMessage, String pModeKey, String backendName) throws EbMS3Exception, MessagingProcessingException {
        try {
            Party from = pModeProvider.getSenderParty(pModeKey);
            Party to = pModeProvider.getReceiverParty(pModeKey);
            backendMessageValidator.validateParties(from, to);

            Party gatewayParty = pModeProvider.getGatewayParty();
            backendMessageValidator.validateInitiatorParty(gatewayParty, from);
            backendMessageValidator.validateResponderParty(gatewayParty, to);

            backendMessageValidator.validatePayloads(userMessage.getPayloadInfo());

            return to;
        } catch (IllegalArgumentException runTimEx) {
            LOG.error(ERROR_SUBMITTING_THE_MESSAGE_STR + userMessage.getMessageInfo().getMessageId() + TO_STR + backendName + "]", runTimEx);
            throw MessagingExceptionFactory.transform(runTimEx, ErrorCode.EBMS_0003);
        }
    }

    private int getMaxAttempts(LegConfiguration legConfiguration) {
        return (legConfiguration.getReceptionAwareness() == null ? 1 : legConfiguration.getReceptionAwareness().getRetryCount()) + 1; // counting retries after the first send attempt
    }

    private void fillMpc(UserMessage userMessage, LegConfiguration legConfiguration, Party to) {
        final Map<Party, Mpc> mpcMap = legConfiguration.getPartyMpcMap();
        String mpc = Ebms3Constants.DEFAULT_MPC;
        if (legConfiguration.getDefaultMpc() != null) {
            mpc = legConfiguration.getDefaultMpc().getQualifiedName();
        }
        if (mpcMap != null && mpcMap.containsKey(to)) {
            mpc = mpcMap.get(to).getQualifiedName();
        }
        userMessage.setMpc(mpc);
    }

    protected Party createNewParty(String mpc) {
        if (mpc == null) {
            return null;
        }
        Party party = new Party();
        Identifier identifier = new Identifier();
        identifier.setPartyId(messageExchangeService.extractInitiator(mpc));
        party.setIdentifiers(Sets.newHashSet(identifier));
        party.setName(messageExchangeService.extractInitiator(mpc));

        return party;
    }
}
