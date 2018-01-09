package eu.domibus.plugin.webService;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import eu.domibus.AbstractIT;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.plugin.webService.generated.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.xml.ws.Holder;
import java.io.IOException;

/**
 * This JUNIT implements the Test cases Download Message-01 and Download Message-02.
 *
 * @author draguio
 * @author martifp
 */
public class DownloadMessageIT extends AbstractIT {


    protected static ConnectionFactory connectionFactory;
    private static boolean initialized;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(SERVICE_PORT);

    @Autowired
    BackendInterface backendWebService;

    @Before
    public void before() throws IOException {
        if (!initialized) {
            insertDataset("downloadMessage.sql");
            initialized = true;
            connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        }
    }

    /**
     * Negative test: the message is not found in the JMS queue and a specific exception is returned.
     *
     * @throws DownloadMessageFault
     */
    @Test(expected = DownloadMessageFault.class)
    public void testDownloadMessageInvalidId() throws DownloadMessageFault {

        // Prepare the request to the backend
        String messageId = "invalid@e-delivery.eu";
        DownloadMessageRequest downloadMessageRequest = createDownloadMessageRequest(messageId);
        Holder<DownloadMessageResponse> downloadMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        try {
            backendWebService.downloadMessage(downloadMessageRequest, downloadMessageResponse, ebMSHeaderInfo);
        } catch (DownloadMessageFault re) {
            String message = "No message with id [invalid@e-delivery.eu] pending for download";
            Assert.assertEquals(message, re.getFaultInfo().getMessage());
            throw re;
        }
        Assert.fail("DownloadMessageFault was expected but was not raised");
    }

    /**
     * Tests that a message is found in the JMS queue and the payload is returned.
     * The tables TB_MESSAGING, TB_MESSAGE_INFO, TB_MESSAGE_LOG, TB_USER_MESSAGE and TB_PART_INFO are involved by this test.
     *
     * @throws DownloadMessageFault
     * @throws JMSException
     */
    @Test
    @Transactional
    public void testDownloadMessageOk() throws Exception {

        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection("domibus", "changeit");

        String messageId = "2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu";

        pushMessage(connection, messageId);

        DownloadMessageRequest downloadMessageRequest = createDownloadMessageRequest(messageId);
        Holder<DownloadMessageResponse> downloadMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        downloadMessageRequestResponse(downloadMessageRequest, downloadMessageResponse, ebMSHeaderInfo);
        Assert.assertFalse(downloadMessageResponse.value.getPayload().isEmpty());
        PayloadType payloadType = downloadMessageResponse.value.getPayload().iterator().next();
        String payload = new String(payloadType.getValue());
        System.out.println("Payload returned [" + payload +"]");
        Assert.assertEquals(payload, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<hello>world</hello>");
    }

    private void downloadMessageRequestResponse(DownloadMessageRequest downloadMessageRequest, Holder<DownloadMessageResponse> downloadMessageResponse, Holder<Messaging> ebMSHeaderInfo) throws DownloadMessageFault {
        try {
            backendWebService.downloadMessage(downloadMessageRequest, downloadMessageResponse, ebMSHeaderInfo);
        } catch (DownloadMessageFault dmf) {
            String message = "Downloading message failed";
            Assert.assertEquals(message, dmf.getMessage());
            throw dmf;
        }
    }

    /**
     * Tests that a message is found in the JMS queue, that the payload is returned but is not the one expected.
     * The tables TB_MESSAGING, TB_MESSAGE_INFO, TB_MESSAGE_LOG, TB_USER_MESSAGE and TB_PART_INFO are involved by this test.
     *
     * @throws DownloadMessageFault
     * @throws JMSException
     */
    @Test
    @Transactional
    public void testDownloadMessageOkPayloadNok() throws Exception {

        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection("domibus", "changeit");

        String messageId = "78a1d578-0cc7-41fb-9f35-86a5b2769a14@domibus.eu";

        pushMessage(connection, messageId);

        DownloadMessageRequest downloadMessageRequest = createDownloadMessageRequest(messageId);
        Holder<DownloadMessageResponse> downloadMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        downloadMessageRequestResponse(downloadMessageRequest, downloadMessageResponse, ebMSHeaderInfo);
        Assert.assertFalse(downloadMessageResponse.value.getPayload().isEmpty());
        PayloadType payloadType = downloadMessageResponse.value.getPayload().iterator().next();
        String payload = new String(payloadType.getValue());
        System.out.println("Payload returned [" + payload +"]");
        Assert.assertNotEquals(payload, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    /**
     * Tests that a message is found in the JMS queue, that the bodyload is returned.
     *
     * @throws DownloadMessageFault
     * @throws JMSException
     */
    @Test
    @Transactional
    public void testDownloadMessageBodyLoad() throws Exception {
        downloadMessage("2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu");
    }

    @Test
    @Transactional
    public void testMessageIdNeedsATrimSpaces() throws Exception {
        downloadMessage("    2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu ");
    }

    @Test
    @Transactional
    public void testMessageIdNeedsATrimTabs() throws Exception {
        downloadMessage("\t2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu\t");
    }

    @Test
    @Transactional
    public void testMessageIdNeedsATrimSpacesAndTabs() throws Exception {
        downloadMessage(" \t 2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu \t ");
    }

    private void downloadMessage(String messageId) throws Exception {
        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection("domibus", "changeit");

        pushMessage(connection, StringUtils.trim(messageId).replace("\t", ""));

        DownloadMessageRequest downloadMessageRequest = createDownloadMessageRequest(messageId);
        Holder<DownloadMessageResponse> downloadMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        downloadMessageRequestResponse(downloadMessageRequest, downloadMessageResponse, ebMSHeaderInfo);
        Assert.assertNotNull(downloadMessageResponse.value.getBodyload());
        PayloadType payloadType = downloadMessageResponse.value.getBodyload();
        String payload = new String(payloadType.getValue());
        System.out.println("Payload returned [" + payload + "]");
        Assert.assertEquals(payload, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<hello>world</hello>");
    }

    private void pushMessage(ActiveMQConnection connection, String messageId) throws Exception {
        connection.start();
        pushQueueMessage(messageId, connection, WS_NOT_QUEUE);
        connection.close();
    }

    private DownloadMessageRequest createDownloadMessageRequest(String messageId) {
        DownloadMessageRequest downloadMessageRequest = new DownloadMessageRequest();
        downloadMessageRequest.setMessageID(messageId);
        return downloadMessageRequest;
    }
}
