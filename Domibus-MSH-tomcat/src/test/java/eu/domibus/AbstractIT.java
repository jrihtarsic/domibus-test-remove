package eu.domibus;

import com.thoughtworks.xstream.XStream;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationType;
import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.configuration.Storage;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.sender.DispatchClientDefaultProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyBuilderImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static eu.domibus.plugin.jms.JMSMessageConstants.MESSAGE_ID;
import static org.awaitility.Awaitility.with;

/**
 * Created by feriaad on 02/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-context.xml")
@DirtiesContext
@Rollback
public abstract class AbstractIT {

    protected static final int SERVICE_PORT = 8892;
    protected static final String BACKEND_SERVICE_ENDPOINT = "http://localhost:" + SERVICE_PORT + "/domibus/services/backend";

    public enum Mode {DATABASE, FILESYSTEM}

    protected static final String WS_NOT_QUEUE = "domibus.notification.webservice";

    protected static final String JMS_NOT_QUEUE_NAME = "domibus.notification.jms";

    protected static final String JMS_BACKEND_IN_QUEUE_NAME = "domibus.backend.jms.inQueue";

    protected static final String JMS_BACKEND_OUT_QUEUE_NAME = "domibus.backend.jms.outQueue";

    protected static final String JMS_BACKEND_REPLY_QUEUE_NAME = "domibus.backend.jms.replyQueue";

    protected static final String JMS_DISPATCH_QUEUE_NAME = "domibus.internal.dispatch.queue";

    private Message message;

    private javax.jms.Connection connection;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    private String queueName;

    private static boolean initialized;

    protected static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractIT.class);

    @Qualifier("domibusJDBC-XADataSource")
    @Autowired
    protected DataSource dataSource;

    @Autowired
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Autowired
    private Storage storage;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @BeforeClass
    public static void init() throws IOException {
        if (!initialized) {
            FileUtils.deleteDirectory(new File("target/temp"));
            System.setProperty("domibus.config.location", new File("target/test-classes").getAbsolutePath());
            initialized = true;
        }
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test_user",
                        "test_password",
                        Collections.singleton(new SimpleGrantedAuthority(eu.domibus.api.security.AuthRole.ROLE_ADMIN.name()))));
    }

    @Before
    public void setDomain() {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
    }

    protected UserMessage getUserMessageTemplate() throws IOException {
        XStream xStream = new XStream();
        return (UserMessage) xStream.fromXML(new ClassPathResource("dataset/messages/UserMessageTemplate.xml").getInputStream());
    }


    protected void waitUntilMessageHasStatus(String messageId, MessageStatus messageStatus) {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(5, TimeUnit.SECONDS).until(messageHasStatus(messageId, messageStatus));
    }

    protected void waitUntilMessageIsAcknowledged(String messageId) {
        waitUntilMessageHasStatus(messageId, MessageStatus.ACKNOWLEDGED);
    }

    protected void waitUntilMessageIsReceived(String messageId) {
        waitUntilMessageHasStatus(messageId, MessageStatus.RECEIVED);
    }

    protected void waitUntilMessageIsInWaitingForRetry(String messageId) {
        waitUntilMessageHasStatus(messageId, MessageStatus.WAITING_FOR_RECEIPT);
    }

    protected Callable<Boolean> messageHasStatus(String messageId, MessageStatus messageStatus) {
        return () -> messageStatus == userMessageLogDao.getMessageStatus(messageId);
    }


    /**
     * Execute the given input stream in the given database connection
     *
     * @param conn
     * @param in
     * @throws SQLException
     */
    private void importSQL(Connection conn, InputStream in, Mode mode) throws SQLException, IOException {
        Scanner s = new Scanner(in);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        Statement st = null;
        try {
            st = conn.createStatement();
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0) {
                    st.execute(line);
                }
            }
        } finally {
            if (st != null) st.close();
        }

        if (Mode.FILESYSTEM.equals(mode)) {
            //write to FS
            String readPayloads = "SELECT * FROM TB_PART_INFO";
            st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(readPayloads);
            while (rs.next()) {
                String filename = domibusProperties.getProperty(Storage.ATTACHMENT_STORAGE_LOCATION) + "/" + UUID.randomUUID().toString() + ".payload";
                FileOutputStream f = new FileOutputStream(filename);
                f.write(rs.getBytes("BINARY_DATA"));
                f.close();
                rs.updateNull("BINARY_DATA");
                rs.updateString("FILENAME", filename);
                rs.updateRow();


            }
            st.close();
        }
    }

    /**
     * Convert the given file to a string
     *
     * @param file
     * @return
     */
    protected String getAS4Response(String file) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = getClass().getClassLoader().getResourceAsStream("dataset/as4/" + file);
            Document doc = db.parse(is);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = null;
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception exc) {
            Assert.fail(exc.getMessage());
            exc.printStackTrace();
        }
        return null;
    }


    protected void insertDataset(String dataset) throws IOException {
        this.insertDataset(dataset, this.getMode());
    }


    /**
     * Insert the given dataset inside the database
     *
     * @param dataset
     */
    protected void insertDataset(String dataset, Mode mode) throws IOException {
        if (Mode.FILESYSTEM.equals(mode)) {
            File target = new File("target/test-classes/dataset/storage");
            if (!target.exists()) {
                target.mkdirs();
            }
            domibusProperties.put(Storage.ATTACHMENT_STORAGE_LOCATION, new File("target/test-classes/dataset/storage").getAbsolutePath());
            storage.initFileSystemStorage();
        }

        try {
            FileInputStream fis = new FileInputStream(new File("target/test-classes/dataset/database/" + dataset).getAbsolutePath());
            importSQL(dataSource.getConnection(), fis, mode);
        } catch (final Exception exc) {
            Assert.fail(exc.getMessage());
            exc.printStackTrace();
        }
    }

    /**
     * The connection must be started and stopped before and after the method call.
     *
     * @param connection
     * @param queueName
     * @return
     * @throws Exception
     */
    protected void pushQueueMessage(String messageId, javax.jms.Connection connection, String queueName) throws Exception {

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(destination);
        // Creates the Message using Spring MessageCreator
//        NotifyMessageCreator messageCreator = new NotifyMessageCreator(messageId, NotificationType.MESSAGE_RECEIVED);
        Message msg = session.createTextMessage();
        msg.setStringProperty(MessageConstants.MESSAGE_ID, messageId);
        msg.setObjectProperty(MessageConstants.NOTIFICATION_TYPE, NotificationType.MESSAGE_RECEIVED.name());
        msg.setStringProperty(MessageConstants.ENDPOINT, "backendInterfaceEndpoint");
        msg.setStringProperty(MessageConstants.FINAL_RECIPIENT, "testRecipient");
        producer.send(msg);
        System.out.println("Message with ID [" + messageId + "] sent in queue!");
        producer.close();
        session.close();

    }

    /**
     * The connection must be started and stopped before and after the method call.
     *
     * @param connection
     * @param queueName
     * @param mSecs
     * @return Message
     * @throws Exception
     */
    protected Message popQueueMessageWithTimeout(javax.jms.Connection connection, String queueName, long mSecs) throws Exception {

        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Destination destination = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(destination);
        Message message = consumer.receive(mSecs);
        if (message != null) {
            System.out.println("Message with ID [:" + message.getStringProperty(MESSAGE_ID) + "] consumed from queue [" + message.getJMSDestination() + "]");
        }
        consumer.close();
        session.close();
        return message;
    }


    protected Mode getMode() {
        return Mode.valueOf(System.getProperty("attachment.mode", Mode.DATABASE.name()));
    }

    //TODO move this method into a class in the domibus-MSH-test module in order to be reused
    public SOAPMessage createSOAPMessage(String dataset) throws SOAPException, IOException, ParserConfigurationException, SAXException {

        MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage message = factory.createMessage();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(getClass().getClassLoader().getResourceAsStream("dataset/as4/" + dataset));
        DOMSource domSource = new DOMSource(document);
        SOAPPart soapPart = message.getSOAPPart();
        soapPart.setContent(domSource);

        AttachmentPart attachment = message.createAttachmentPart();
        attachment.setContent(Base64.decodeBase64("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=".getBytes()), "text/xml");
        attachment.setContentId("cid:message");
        message.addAttachmentPart(attachment);

        message.setProperty(DispatchClientDefaultProvider.PMODE_KEY_CONTEXT_PROPERTY, "blue_gw:red_gw:testService1:tc1Action::pushTestcase1tc1Action");
        return message;
    }

    protected SoapMessage createSoapMessage(String dataset) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("dataset/as4/" + dataset);

        SoapMessage sm = new SoapMessage(new MessageImpl());
        sm.setContent(InputStream.class, is);
        InterceptorChain ic = new PhaseInterceptorChain((new PhaseManagerImpl()).getOutPhases());
        sm.setInterceptorChain(ic);
        ExchangeImpl exchange = new ExchangeImpl();
        Bus bus = new ExtensionManagerBus();
        bus.setExtension(new PolicyBuilderImpl(bus), PolicyBuilder.class);
        exchange.put(Bus.class, bus);
        sm.setExchange(exchange);

        return sm;
    }

}
