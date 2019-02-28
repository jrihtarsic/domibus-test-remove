package eu.domibus.jms.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPElement;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

public class SendJMSMessageOnTomcatPRPA_IN201306UV02 {

    public static void main(String[] args) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61617");
        Connection connection;
        MessageProducer producer;
        try {
            connection = connectionFactory.createConnection("domibus", "changeit"); //username and password of the default JMS broker

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("domibus.backend.jms.inQueue");
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            MapMessage messageMap = session.createMapMessage();

            // Declare message as submit
            messageMap.setStringProperty("username", "plugin_admin");
            messageMap.setStringProperty("password", "Test123456!");

            messageMap.setStringProperty("messageType", "submitMessage");
            messageMap.setStringProperty("messageId", UUID.randomUUID().toString());
            // Uncomment to test refToMessageId that is too long, i.e. > 255
            // messageMap.setStringProperty("refToMessageId", "0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-0079a47e-aesasa");

            // Set up the Communication properties for the message
            messageMap.setStringProperty("service", "urn:ihe:iti:2007:CrossGatewayQuery");
            messageMap.setStringProperty("serviceType", "eHSDI");

            messageMap.setStringProperty("action", "urn:ihe:iti:2007:CrossGatewayQuery");
            messageMap.setStringProperty("conversationId", "123");
            //messageMap.setStringProperty("fromPartyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-blue");
            //messageMap.setStringProperty("fromPartyType", ""); // Mandatory but empty here because it is in the value of the party ID
            messageMap.setStringProperty("fromPartyId", "domibus-blue");
            messageMap.setStringProperty("fromPartyType", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:eHSDI"); // Mandatory

            messageMap.setStringProperty("fromRole", "urn:ihe:iti:2018:Requester");

            //messageMap.setStringProperty("toPartyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-red");
            //messageMap.setStringProperty("toPartyType", ""); // Mandatory but empty here because it is in the value of the party ID
            messageMap.setStringProperty("toPartyId", "domibus-red");
            messageMap.setStringProperty("toPartyType", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:eHSDI"); // Mandatory

            messageMap.setStringProperty("toRole", "urn:ihe:iti:2018:Provider");

            messageMap.setStringProperty("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1");
            messageMap.setStringProperty("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4");
            messageMap.setStringProperty("protocol", "AS4");

//            messageMap.setJMSCorrelationID("12345");
            //Set up the payload properties
            messageMap.setStringProperty("totalNumberOfPayloads", "1");

            messageMap.setStringProperty("payload_1_mimeContentId", "");
            messageMap.setStringProperty("payload_1_description", "message");
            messageMap.setStringProperty("payload_1_mimeType", "application/xml");
            messageMap.setStringProperty("p1InBody", "true"); // If true payload_1 will be sent in the body of the AS4 message. Only XML payloads may be sent in the AS4 message body. Optional


//            messageMap.setStringProperty("payload_2_description", "message");
//            messageMap.setStringProperty("payload_2_mimeContentId", "cid:message");
//            messageMap.setStringProperty("payload_2_mimeType", "text/xml");

            final Charset utf8Encoding = Charset.forName("UTF-8");
            final String saml1 = IOUtils.toString(new ClassPathResource("saml3.xml").getInputStream(), utf8Encoding);
//            final String saml2 = IOUtils.toString(new ClassPathResource("saml2.xml").getInputStream(), utf8Encoding);


            messageMap.setStringProperty("property_saml1", saml1);
//            messageMap.setStringProperty("property_saml2", saml2);


            //send the payload in the JMS message as byte array
            String pay1 = IOUtils.toString(new ClassPathResource("PRPA_IN201306UV02.xml").getInputStream(), utf8Encoding);
            byte[] payload = pay1.getBytes();
            messageMap.setBytes("payload_1", payload);
//            messageMap.setBytes("payload_2", payload);


            //send the payload as a file system reference
//            messageMap.setStringProperty("payload_1_fileName", "1_2GB.zip");
//            messageMap.setString("payload_1", "file:////C:/DEV/1_2GB.zip");

            producer.send(messageMap);

            connection.close();
        } catch (JMSException | IOException e) {
            e.printStackTrace();
        }
    }

    protected void test(SOAPElement security) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();


        /* parse existing file to DOM */
        Document document = documentBuilder.parse(new ClassPathResource("saml1.xml").getInputStream());

        Document securityDoc = security.getOwnerDocument();
        Node newNode = securityDoc.importNode(document.getFirstChild(), true);

        //Add the Node
        security.appendChild(newNode);
    }


}