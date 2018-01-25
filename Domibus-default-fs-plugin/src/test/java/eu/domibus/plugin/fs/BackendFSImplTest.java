package eu.domibus.plugin.fs;

import eu.domibus.common.*;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.MessageLister;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@RunWith(JMockit.class)
public class BackendFSImplTest {

    private static final String TEXT_XML = "text/xml";

    @Injectable
    protected MessageRetriever<Submission> messageRetriever;

    @Injectable
    protected MessageSubmitter<Submission> messageSubmitter;

    @Injectable
    private MessageLister lister;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    private FSMessageTransformer defaultTransformer;

    @Injectable
    String name = "fsplugin";


    @Tested
    BackendFSImpl backendFS;

    private FileObject rootDir;

    private FileObject incomingFolder;
    
    private FileObject outgoingFolder;
    
    private FileObject sentFolder;

    private FileObject failedFolder;

    @Before
    public void setUp() throws org.apache.commons.vfs2.FileSystemException {
        String location = "ram:///BackendFSImplTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        incomingFolder = rootDir.resolveFile(FSFilesManager.INCOMING_FOLDER);
        incomingFolder.createFolder();
        
        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        outgoingFolder.createFolder();
        
        sentFolder = rootDir.resolveFile(FSFilesManager.SENT_FOLDER);
        sentFolder.createFolder();

        failedFolder = rootDir.resolveFile(FSFilesManager.FAILED_FOLDER);
        failedFolder.createFolder();
    }

    @After
    public void tearDown() throws FileSystemException {
        incomingFolder.close();
        outgoingFolder.close();
        sentFolder.close();
        
        rootDir.deleteAll();
        rootDir.close();
    }


    @Test
    public void testDeliverMessage_NormalFlow(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(payloadContent.getBytes(), TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, dataHandler));

        new Expectations(1, backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(fsPayloads, userMessage);
            
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;
        }};

        backendFS.deliverMessage(messageId);

        // Assert results
        FileObject[] files = incomingFolder.findFiles(new FileTypeSelector(FileType.FILE));
        Assert.assertEquals(1, files.length);
        FileObject fileMessage = files[0];

        Assert.assertEquals(messageId + ".xml", fileMessage.getName().getBaseName());
        Assert.assertEquals(payloadContent, IOUtils.toString(fileMessage.getContent().getInputStream()));
        fileMessage.delete();
        fileMessage.close();
    }

    @Test
    public void testDeliverMessage_MultiplePayloads(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        final String messageContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGludm9pY2U+aGVsbG88L2ludm9pY2U+";
        final String invoiceContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";

        final DataHandler messageHandler = new DataHandler(new ByteArrayDataSource(messageContent.getBytes(), TEXT_XML));
        final DataHandler invoiceHandler = new DataHandler(new ByteArrayDataSource(invoiceContent.getBytes(), TEXT_XML));
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, messageHandler));
        fsPayloads.put("cid:invoice", new FSPayload(TEXT_XML, invoiceHandler));

        new Expectations(1, backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(fsPayloads, userMessage);
            
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;
        }};

        backendFS.deliverMessage(messageId);

        // Assert results
        FileObject[] files = incomingFolder.findFiles(new FileTypeSelector(FileType.FILE));
        Arrays.sort(files, new Comparator<FileObject>() {
            @Override
            public int compare(FileObject o1, FileObject o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Assert.assertEquals(2, files.length);

        FileObject fileMessage0 = files[0];
        Assert.assertEquals("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu_invoice.xml",
                fileMessage0.getName().getBaseName());
        Assert.assertEquals(invoiceContent, IOUtils.toString(fileMessage0.getContent().getInputStream()));
        fileMessage0.delete();
        fileMessage0.close();

        FileObject fileMessage1 = files[1];
        Assert.assertEquals("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu_message.xml",
                fileMessage1.getName().getBaseName());
        Assert.assertEquals(messageContent, IOUtils.toString(fileMessage1.getContent().getInputStream()));
        fileMessage1.delete();
        fileMessage1.close();
    }

    @Test(expected = FSPluginException.class)
    public void testDeliverMessage_MessageNotFound(@Injectable final FSMessage fsMessage) throws MessageNotFoundException {

        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";

        new Expectations(1, backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new MessageNotFoundException("message not found");
        }};

        backendFS.deliverMessage(messageId);
    }
    
    @Test(expected = FSPluginException.class)
    public void testDeliverMessage_FSSetUpException(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(payloadContent.getBytes(), TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, dataHandler));

        new Expectations(1, backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(fsPayloads, userMessage);

            fsFilesManager.setUpFileSystem(null);
            result = new FSSetUpException("Test-forced exception");
        }};

        backendFS.deliverMessage(messageId);
    }
    
    @Test(expected = FSPluginException.class)
    public void testDeliverMessage_IOException(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        // the null causes an IOException
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource((byte[])null, TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, dataHandler));

        new Expectations(1, backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(fsPayloads, userMessage);
            
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;
        }};

        backendFS.deliverMessage(messageId);
    }

    @Test
    public void testGetMessageSubmissionTransformer() {
        MessageSubmissionTransformer<FSMessage> result = backendFS.getMessageSubmissionTransformer();
        
        Assert.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testGetMessageRetrievalTransformer() {
        MessageRetrievalTransformer<FSMessage> result = backendFS.getMessageRetrievalTransformer();
        
        Assert.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testMessageStatusChanged() throws FSSetUpException, FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.READY_TO_SEND);
        event.setToStatus(MessageStatus.SEND_ENQUEUED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));
        
        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.READY_TO_SEND");
        
        new Expectations(1, backendFS) {{
//            unneeded when main location contains file
//            fsPluginProperties.getDomains();
//            result = Collections.emptySet();
            
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;
            
            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };
        }};
        
        backendFS.messageStatusChanged(event);

        contentFile.close();
        
        new VerificationsInOrder(1) {{
            fsFilesManager.renameFile(contentFile, "content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.SEND_ENQUEUED");
        }};
    }

    @Test
    public void testResolveDomain_1() {
        String serviceDomain1 = "ODRDocumentInvoiceService123";
        String actionDomain1 = "PrintA";

        final List<String> domains = new ArrayList<>();
        domains.add("DOMAIN1");

        new Expectations(1, backendFS) {{
            fsPluginProperties.getDomains();
            result = domains;

            fsPluginProperties.getExpression("DOMAIN1");
            result = "ODRDocumentInvoiceService.*#Print.?";
        }};

        String result = backendFS.resolveDomain(serviceDomain1, actionDomain1);
        Assert.assertEquals("DOMAIN1", result);
    }

    @Test
    public void testResolveDomain_2() {
        String serviceDomain2 = "BRISReceptionService";
        String actionDomain2 = "SendEmailAction";
        String actionDomain2a = "ReceiveBillAction";

        final List<String> domains = new ArrayList<>();
        domains.add("DOMAIN1");
        domains.add("DOMAIN2");

        new Expectations(1, backendFS) {{
            fsPluginProperties.getDomains();
            result = domains;

            fsPluginProperties.getExpression("DOMAIN2");
            result = "BRISReceptionService#.*";
        }};

        String result = backendFS.resolveDomain(serviceDomain2, actionDomain2);
        Assert.assertEquals("DOMAIN2", result);

        result = backendFS.resolveDomain(serviceDomain2, actionDomain2a);
        Assert.assertEquals("DOMAIN2", result);
    }

    @Test
    public void testResolveDomain_WithoutMatch() {
        String serviceDomain1 = "ODRDocumentInvoiceService123";
        String actionDomain1 = "PrintA";

        String serviceWithoutMatch = "FSService123";
        String actionWithoutMatch = "SomeAction";

        final List<String> domains = new ArrayList<>();
        domains.add("DOMAIN1");
        domains.add("DOMAIN2");

        new Expectations(1, backendFS) {{
            fsPluginProperties.getDomains();
            result = domains;

            fsPluginProperties.getExpression("DOMAIN1");
            result = "ODRDocumentInvoiceService.*#Print.?";

            fsPluginProperties.getExpression("DOMAIN2");
            result = "BRISReceptionService#.*";
        }};

        String result = backendFS.resolveDomain(serviceWithoutMatch, actionWithoutMatch);
        Assert.assertNull(result);

        result = backendFS.resolveDomain(serviceDomain1, actionWithoutMatch);
        Assert.assertNull(result);

        result = backendFS.resolveDomain(serviceWithoutMatch, actionDomain1);
        Assert.assertNull(result);
    }

    @Test
    public void testResolveDomain_bdxNoprocessTC1Leg1() {
        String service = "bdx:noprocess";
        String action = "TC1Leg1";

        final List<String> domains = new ArrayList<>();
        domains.add("DOMAIN1");

        new Expectations(1, backendFS) {{
            fsPluginProperties.getDomains();
            result = domains;

            fsPluginProperties.getExpression("DOMAIN1");
            result = "bdx:noprocess#TC1Leg1";
        }};

        String result = backendFS.resolveDomain(service, action);
        Assert.assertEquals("DOMAIN1", result);
    }

    @Test
    public void testMessageStatusChanged_SendSuccessDelete() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.ACKNOWLEDGED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.ACKNOWLEDGED");
        
        new Expectations(1, backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;
            
            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };
            
            fsPluginProperties.isSentActionDelete(null);
            result = true;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();
        
        new VerificationsInOrder(1) {{
            fsFilesManager.deleteFile(contentFile);
        }};
    }

    @Test
    public void testMessageStatusChanged_SendSuccessArchive() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.ACKNOWLEDGED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.ACKNOWLEDGED");
        
        new Expectations(1, backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;
            
            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };
            
            fsPluginProperties.isSentActionDelete(null);
            result = false;
            
            fsPluginProperties.isSentActionArchive(null);
            result = true;
            
            fsFilesManager.getEnsureChildFolder(rootDir, "/BackendFSImplTest/SENT/");
            result = sentFolder;
            
        }};
        
        backendFS.messageStatusChanged(event);

        contentFile.close();
        
        new VerificationsInOrder(1) {{
            fsFilesManager.moveFile(contentFile, with(new Delegate<FileObject>() {
              void delegate(FileObject file) throws IOException {
                     Assert.assertNotNull(file);
                     Assert.assertEquals("ram:///BackendFSImplTest/SENT/content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml", file.getName().getURI());
                 }  
            }));
        }};
    }

    @Test
    public void testMessageStatusChanged_SendFailedDelete() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.SEND_ENQUEUED");

        new Expectations(1, backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };

            fsPluginProperties.isFailedActionDelete(null);
            result = true;

            fsFilesManager.getEnsureChildFolder(rootDir, "/BackendFSImplTest/FAILED/");
            result = failedFolder;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder(1) {{
            fsFilesManager.deleteFile(contentFile);
        }};
    }

    @Test
    public void testMessageStatusChanged_SendFailedArchive() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.SEND_ENQUEUED");

        new Expectations(1, backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };

            fsPluginProperties.isFailedActionDelete(null);
            result = false;

            fsPluginProperties.isFailedActionArchive(null);
            result = true;

            fsFilesManager.getEnsureChildFolder(rootDir, "/BackendFSImplTest/FAILED/");
            result = failedFolder;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder(1) {{
            fsFilesManager.moveFile(contentFile, with(new Delegate<FileObject>() {
                void delegate(FileObject file) throws IOException {
                    Assert.assertNotNull(file);
                    Assert.assertEquals("ram:///BackendFSImplTest/FAILED/content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml", file.getName().getURI());
                }
            }));
        }};
    }

    @Test
    public void testMessageStatusChanged_SendFailedErrorFile() throws IOException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.SEND_ENQUEUED");

        final List<ErrorResult> errorList = new ArrayList<>();
        ErrorResultImpl errorResult = new ErrorResultImpl();
        errorResult.setErrorCode(ErrorCode.EBMS_0001);
        errorList.add(errorResult);

        new Expectations(1, backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };

            fsPluginProperties.isFailedActionDelete(null);
            result = true;

            fsFilesManager.getEnsureChildFolder(rootDir, "/BackendFSImplTest/FAILED/");
            result = failedFolder;

            backendFS.getErrorsForMessage("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
            result = errorList;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder(1) {{
            fsFilesManager.deleteFile(contentFile);
        }};
    }

}
