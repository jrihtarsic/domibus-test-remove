package eu.domibus.jms.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.UUID;

public class SendJMSMessageOnTomcat {

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

           /* ---- public static void sendMessageJMS20(ConnectionFactory connectionFactory, Queue queue,
    String text) {
       try (JMSContext context = connectionFactory.createContext();){

            MapMessage map = context.createMapMessage();
           map.setBytes("payload_1", text.getBytes());

            context.createProducer()
           .setProperty("fromPartyId", "ADALUPLDAT")
           .setProperty("fromPartyType", "urn:oasis:names:tc:ebcore:partyid-type:PLDA_TEST")
           .setProperty("toPartyId", "RSSBUSAS4")
           .setProperty("toPartyType", "urn:oasis:names:tc:ebcore:partyid-type:rssbusas4ADA")
           .setProperty("service", "zfService")
           .setProperty("action", "zfAction")
           .setProperty("fromRole", "Initiator")
           .setProperty("toRole", "Responder")
           .setProperty("messageType", "submitMessage")
           .setProperty("totalNumberOfPayloads", 1)
           .setProperty("payload_1_mimeType", "application/xml")
                 .send(queue, map);
       } catch (JMSRuntimeException ex) {
          // handle exception (details omitted)
       } catch (JMSException e) {
        }
    }*/

            MapMessage messageMap = session.createMapMessage();

            // Declare message as submit
            messageMap.setStringProperty("username", "plugin_admin");
            messageMap.setStringProperty("password", "123456");

            messageMap.setStringProperty("messageType", "submitMessage");
            messageMap.setStringProperty("messageId", UUID.randomUUID().toString());
            // Uncomment to test refToMessageId that is too long, i.e. > 255
            // messageMap.setStringProperty("refToMessageId", "0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-ae1a-4c1b-ad62-2b18ee57e39e@domibus.eu0079a47e-0079a47e-aesasa");

            // Set up the Communication properties for the message
            messageMap.setStringProperty("service", "bdx:noprocess");
            //essageMap.setStringProperty("service", "zfService");@@
            //messageMap.setStringProperty("serviceType", "noSecurity");

           // messageMap.setStringProperty("ECIDContext", "1.6a497516-a92b-417e-84c0-94e4176796a6-00000606;kZhgv0ZGZKSULGSPXKTPJHSRo4USpLO");
            messageMap.setStringProperty("serviceType", "tc1");
           // messageMap.setStringProperty("action", "zfAction");
           messageMap.setStringProperty("action", "TC1Leg1");
            messageMap.setStringProperty("conversationId", "123");
            messageMap.setStringProperty("fromPartyId", "domibus-blue");
            //messageMap.setStringProperty("fromPartyType", "urn:oasis:names:tc:ebcore:partyid-type:PLDA_TEST"); // Mandatory but empty here because it is in the value of the party ID
            //messageMap.setStringProperty("fromPartyId", "domibus-blue");  @@
            messageMap.setStringProperty("fromPartyType", "urn:oasis:names:tc:ebcore:partyid-type:unregistered"); // Mandatory  @@

           messageMap.setStringProperty("fromRole", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
           //messageMap.setStringProperty("fromRole", "Initiator");
            messageMap.setStringProperty("toPartyId", "domibus-red");
          //  messageMap.setStringProperty("toPartyType", "urn:oasis:names:tc:ebcore:partyid-type:rssbusas4ADA"); // Mandatory but empty here because it is in the value of the party ID
           /* messageMap.setStringProperty("toPartyId", "domibus-red");  @@// Mandatory  @@ */
            messageMap.setStringProperty("toPartyType", "urn:oasis:names:tc:ebcore:partyid-type:unregistered");

           messageMap.setStringProperty("toRole", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder");
            //messageMap.setStringProperty("toRole", "Responder");
            messageMap.setStringProperty("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1");
            messageMap.setStringProperty("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4");
            messageMap.setStringProperty("protocol", "AS4");

            messageMap.setJMSCorrelationID("12345");
            //Set up the payload properties
            messageMap.setStringProperty("totalNumberOfPayloads", "1");
            messageMap.setStringProperty("payload_1_description", "message");
            //messageMap.setStringProperty("payload_1_mimeContentId", "cid:message");
            messageMap.setStringProperty("payload_1_mimeType", "application/xml");

            //messageMap.setStringProperty("p1InBody", "true"); // If true payload_1 will be sent in the body of the AS4 message. Only XML payloads may be sent in the AS4 message body. Optional

            //send the payload in the JMS message as byte array
            String pay1 = "<test>test</test>";
            byte[] payload = pay1.getBytes();
            messageMap.setBytes("payload_1", payload);


            //send the payload as a file system reference
            //messageMap.setStringProperty("payload_1_fileName", "testJMS.txt");
            //messageMap.setString("payload_1", "file:////C:/DEV/testJMS.txt");

            producer.send(messageMap);

            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}