package eu.domibus.jms.weblogic;

import eu.domibus.api.cluster.CommandService;
import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.jms.JMSDestinationHelper;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.jms.spi.InternalJMSDestination;
import eu.domibus.jms.spi.InternalJmsMessage;
import eu.domibus.jms.spi.helper.JMSSelectorUtil;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jms.core.JmsOperations;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.naming.InitialContext;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Cosmin Baciu on 30-Sep-16.
 */
@RunWith(JMockit.class)
public class JMSManagerWeblogicTest {

    @Tested
    InternalJMSManagerWeblogic jmsManagerWeblogic;

    @Injectable
    JMXHelper jmxHelper;

    @Injectable
    JMXTemplate jmxTemplate;

    @Injectable
    CommandService commandService;

    @Injectable
    private JmsOperations jmsSender;

    @Injectable
    JMSDestinationHelper jmsDestinationHelper;

    @Injectable
    JMSSelectorUtil jmsSelectorUtil;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void testGetQueueName() throws Exception {
        String queueName = jmsManagerWeblogic.getShortDestName("JmsModule!DomibusNotifyBackendEtrustexQueue");
        Assert.assertEquals(queueName, "DomibusNotifyBackendEtrustexQueue");

        queueName = jmsManagerWeblogic.getShortDestName("DomibusNotifyBackendEtrustexQueue");
        Assert.assertEquals(queueName, "DomibusNotifyBackendEtrustexQueue");
    }


    @Test
    public void testGetMessagesFromDestinationNOk(@Mocked final ObjectName destination,
                                                  final @Injectable MBeanServerConnection mbsc,
                                                  final @Injectable CompositeData data1,
                                                  final @Injectable CompositeData data2,
                                                  final @Injectable InternalJmsMessage internalJmsMessage1) throws Exception {
        final String selector = "";
        final CompositeData[] compositeDatas = new CompositeData[]{data1, data2};

        new Expectations(jmsManagerWeblogic) {{
            mbsc.invoke(destination, "getCursorSize", withAny(new Object[]{""}), withAny(new String[]{String.class.getName()}));
            result = 2L;

            mbsc.invoke(destination, "getItems", withAny(new Object[]{"", new Long(0),
                    new Integer(2)}), withAny(new String[]{String.class.getName(), Long.class.getName(), Integer.class.getName()}));
            result = compositeDatas;

            jmsManagerWeblogic.getInternalJmsMessage(destination, mbsc, anyString, data1);
            result = new RuntimeException("Simulating a message conversion error");

            jmsManagerWeblogic.getInternalJmsMessage(destination, mbsc, anyString, data2);
            result = internalJmsMessage1;
        }};

        try {
            jmsManagerWeblogic.doGetMessagesFromDestination(mbsc, selector, destination);
        } catch (RuntimeException runEx) {
            assertEquals("Simulating a message conversion error", runEx.getMessage());
        }
    }

    @Test
    public void testGetQueueMap(@Mocked final ObjectName drs,
                                @Mocked final ObjectName config,
                                @Mocked final ObjectName configJmsResource,
                                @Mocked final ObjectName configJmsSystemResource,
                                @Mocked final ObjectName configQueue,
                                @Mocked final ObjectName distributedQueue,
                                @Mocked final ObjectName uniformDistributedQueue,
                                final @Injectable MBeanServerConnection mbsc) throws Exception {
        final ObjectName[] configJmsSystemResources = new ObjectName[]{configJmsSystemResource};
        final ObjectName[] configQueues = new ObjectName[]{configQueue};
        final ObjectName[] distributedQueues = new ObjectName[]{distributedQueue};
        final ObjectName[] uniformDistributedQueues = new ObjectName[]{uniformDistributedQueue};

        new Expectations(jmsManagerWeblogic) {{
            jmxHelper.getDomainRuntimeService();
            result = drs;

            mbsc.getAttribute(drs, "DomainConfiguration");
            result = config;

            mbsc.getAttribute(config, "JMSSystemResources");
            result = configJmsSystemResources;

            mbsc.getAttribute(configJmsSystemResource, "JMSResource");
            result = configJmsResource;

            mbsc.getAttribute(configJmsResource, "Queues");
            result = configQueues;

            mbsc.getAttribute(configQueue, "Name");
            result = "myqueue";

            mbsc.getAttribute(configJmsResource, "DistributedQueues");
            result = distributedQueues;

            mbsc.getAttribute(distributedQueue, "Name");
            result = "mydistributedQueue";

            mbsc.getAttribute(configJmsResource, "UniformDistributedQueues");
            result = uniformDistributedQueues;

            mbsc.getAttribute(uniformDistributedQueue, "Name");
            result = "myuniformDistributedQueue";

        }};

        final Map<String, ObjectName> queueMap = jmsManagerWeblogic.getQueueMap(mbsc);
        assertNotNull(queueMap);
        assertEquals(queueMap.size(), 3);
    }

    @Test
    public void testGetDestinationsSingleServer(@Mocked final ObjectName server1,
                                                @Mocked final ObjectName jmsRuntime,
                                                @Mocked final ObjectName jmsServer,
                                                @Mocked final ObjectName jmsDestination,
                                                @Mocked final ObjectName configQueue,
                                                final @Injectable MBeanServerConnection mbsc) throws Exception {
        final ObjectName[] servers = new ObjectName[]{server1};
        final ObjectName[] jmsServers = new ObjectName[]{jmsServer};
        final ObjectName[] jmsDestinations = new ObjectName[]{jmsDestination};
        final Map<String, ObjectName> queueMap = new HashMap<>();
        final String queueName = "DomibusDLQ";
        queueMap.put(queueName, configQueue);

        final String moduleAndQueueName = "eDeliveryModule!DomibusDLQ";

        new Expectations(jmsManagerWeblogic) {{
            ObjectName drs = jmxHelper.getDomainRuntimeService();
            result = drs;

            mbsc.getAttribute(drs, "ServerRuntimes");
            result = servers;

            mbsc.getAttribute(server1, "JMSRuntime");
            result = jmsRuntime;

            mbsc.getAttribute(jmsRuntime, "JMSServers");
            result = jmsServers;

            mbsc.getAttribute(jmsServer, "Destinations");
            result = jmsDestinations;

            mbsc.getAttribute(jmsDestination, "Name");
            result = moduleAndQueueName;

            jmsManagerWeblogic.getQueueMap(mbsc);
            result = queueMap;

            mbsc.getAttribute(configQueue, "JNDIName");
            result = "jms/domibus.DLQ";

            mbsc.getAttribute(jmsDestination, "MessagesCurrentCount");
            result = 2L;

        }};

        final Map<String, InternalJMSDestination> destinations = jmsManagerWeblogic.findDestinationsGroupedByFQName(mbsc);
        assertNotNull(destinations);
        final InternalJMSDestination internalJmsDestination = destinations.get(moduleAndQueueName);
        assertNotNull(internalJmsDestination);

        assertEquals(internalJmsDestination.getName(), queueName);
        assertEquals(internalJmsDestination.getProperty("ObjectName"), jmsDestination);
        assertEquals(internalJmsDestination.getProperty("Jndi"), "jms/domibus.DLQ");
        assertEquals(internalJmsDestination.getNumberOfMessages(), 2L);

    }

    @Test
    public void testGetDestinationsCluster(@Mocked final ObjectName server1,
                                           @Mocked final ObjectName jmsRuntime,
                                           @Mocked final ObjectName jmsServer,
                                           @Mocked final ObjectName jmsDestination,
                                           @Mocked final ObjectName configQueue,
                                           final @Injectable MBeanServerConnection mbsc) throws Exception {
        final ObjectName[] servers = new ObjectName[]{server1};
        final ObjectName[] jmsServers = new ObjectName[]{jmsServer};
        final ObjectName[] jmsDestinations = new ObjectName[]{jmsDestination};
        final Map<String, ObjectName> queueMap = new HashMap<>();
        final String queueName = "DomibusDLQ";
        queueMap.put(queueName, configQueue);


        new Expectations(jmsManagerWeblogic) {{
            ObjectName drs = jmxHelper.getDomainRuntimeService();
            result = drs;

            mbsc.getAttribute(drs, "ServerRuntimes");
            result = servers;

            mbsc.getAttribute(server1, "JMSRuntime");
            result = jmsRuntime;

            mbsc.getAttribute(jmsRuntime, "JMSServers");
            result = jmsServers;

            mbsc.getAttribute(jmsServer, "Destinations");
            result = jmsDestinations;

            mbsc.getAttribute(jmsDestination, "Name");
            result = "eDeliveryModule!eDeliveryJMS@ms1@DomibusDLQ";

            jmsManagerWeblogic.getQueueMap(mbsc);
            result = queueMap;

            mbsc.getAttribute(configQueue, "JNDIName");
            result = "jms/domibus.DLQ";

            mbsc.getAttribute(jmsDestination, "MessagesCurrentCount");
            result = 2L;

        }};

        final Map<String, InternalJMSDestination> destinations = jmsManagerWeblogic.findDestinationsGroupedByFQName(mbsc);
        assertNotNull(destinations);
        String queueNameWithServerName = "ms1@DomibusDLQ";
        final InternalJMSDestination internalJmsDestination = destinations.get(queueNameWithServerName);
        assertNotNull(internalJmsDestination);

        assertEquals(internalJmsDestination.getName(), queueNameWithServerName);
        assertEquals(internalJmsDestination.getFullyQualifiedName(), "eDeliveryJMS@ms1@DomibusDLQ");
        assertEquals(internalJmsDestination.getProperty("ObjectName"), jmsDestination);
        assertEquals(internalJmsDestination.getProperty("Jndi"), "jms/domibus.DLQ");
        assertEquals(internalJmsDestination.getNumberOfMessages(), 2L);

    }

    @Test
    public void testConvertMessage(final @Injectable CompositeData data) throws Exception {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("jms/WebLogicJMSMessageXML.xml");
        final String jmsMessageXML = IOUtils.toString(xmlStream);

        new Expectations() {{
            data.get("MessageXMLText");
            result = jmsMessageXML;

        }};

        InternalJmsMessage internalJmsMessage = jmsManagerWeblogic.convertMessage(data);

        assertEquals(internalJmsMessage.getTimestamp().getTime(), 1481721027366L);
        assertEquals(internalJmsMessage.getId(), "ID:<704506.1481721027366.0>");
        assertEquals(internalJmsMessage.getContent(), "mycontent");
        assertEquals(internalJmsMessage.getType(), "myJMSType");

        Map<String, Object> properties = internalJmsMessage.getProperties();
        assertEquals(properties.get("JMSType"), "myJMSType");
        assertEquals(properties.get("originalQueue"), "DomibusErrorNotifyProducerQueue");
    }


    @Test
    public void testBrowseMessagesMessages(final @Injectable InternalJMSDestination internalJmsDestination,
                                           final @Injectable ObjectName destination,
                                           final @Injectable List<InternalJmsMessage> messageSPIs) throws Exception {
        final String source = "myqueue";
        final String jmsType = "message";
        final Date fromDate = new Date();
        final Date toDate = new Date();
        final String selectorClause = "mytype = 'message'";

        new Expectations(jmsManagerWeblogic) {{

            jmsManagerWeblogic.getInternalJMSDestination(source);
            result = internalJmsDestination;

            internalJmsDestination.getType();
            result = "Queue";

            jmsSelectorUtil.getSelector(withAny(new HashMap<String, Object>()));
            result = null;

            internalJmsDestination.getProperty("ObjectName");
            result = destination;

            jmsManagerWeblogic.getMessagesFromDestination(destination, anyString);
            result = messageSPIs;

        }};

        List<InternalJmsMessage> messages = jmsManagerWeblogic.browseMessages(source, jmsType, fromDate, toDate, selectorClause);
        assertEquals(messages, messageSPIs);

        new Verifications() {{
            Map<String, Object> criteria = new HashMap<>();
            jmsSelectorUtil.getSelector(criteria = withCapture());

            assertEquals(criteria.get("JMSType"), jmsType);
            assertEquals(criteria.get("JMSTimestamp_from"), fromDate.getTime());
            assertEquals(criteria.get("JMSTimestamp_to"), toDate.getTime());
            assertEquals(criteria.get("selectorClause"), selectorClause);
        }};
    }

    @Test
    public void testGetMessage(final @Injectable InternalJMSDestination internalJmsDestination,
                               final @Injectable ObjectName destination,
                               final @Injectable InternalJmsMessage jmsMessage) throws Exception {

        final String sourceQueue = "eDeliveryJMS@ms2@machineName@DomibusNotifyBackendQueue";
        final String messageId = "id1";

        new Expectations(jmsManagerWeblogic) {{

            jmsManagerWeblogic.getInternalJMSDestinations(sourceQueue);
            result = internalJmsDestination;

            internalJmsDestination.getProperty("ObjectName");
            result = destination;

            jmsManagerWeblogic.getMessageFromDestination(withAny(destination), messageId);
            result = jmsMessage;
        }};

        final InternalJmsMessage jmsMsg = jmsManagerWeblogic.getMessage(sourceQueue, messageId);

        new Verifications() {{
            String capturedMessageId = null;
            ObjectName capturedDestination = null;

            jmsManagerWeblogic.getMessageFromDestination(capturedDestination = withCapture(), capturedMessageId = withCapture());
            assertTrue(destination == capturedDestination);
            assertEquals(messageId, capturedMessageId);
            assertEquals(jmsMsg, jmsMessage);
        }};
    }

    @Test
    public void testMoveMessages(final @Injectable ObjectName sourceObjectName, final @Injectable ObjectName destinationObjectName) throws Exception {

        final String sourceQueue = "sourceQueue";
        final String destinationQueue = "destinationQueue";
        final String[] messageIds = new String[]{"1"};

        new Expectations(jmsManagerWeblogic) {{

            jmsManagerWeblogic.getMessageDestinationName(sourceQueue);
            result = sourceObjectName;

            jmsManagerWeblogic.getMessageDestinationName(destinationQueue);
            result = destinationObjectName;

            jmsManagerWeblogic.moveMessages(sourceObjectName, destinationObjectName, anyString);
            result = 1;

            jmsSelectorUtil.getSelector(messageIds);
            result = "myselector";

        }};

        jmsManagerWeblogic.moveMessages(sourceQueue, destinationQueue, messageIds);

        new Verifications() {{
            ObjectName capturedSource = null;
            ObjectName capturedDestination = null;
            String capturedSelector = null;

            jmsManagerWeblogic.moveMessages(capturedSource = withCapture(), capturedDestination = withCapture(), capturedSelector = withCapture());
            assertTrue(capturedSource == sourceObjectName);
            assertTrue(capturedDestination == destinationObjectName);
            assertEquals(capturedSelector, "myselector");
        }};
    }

    @Test
    public void testDeleteMessages(final @Injectable ObjectName sourceObjectName) throws Exception {

        final String sourceQueue = "sourceQueue";
        final String myselector = "myselector";
        final String[] messageIds = new String[]{"1"};

        new Expectations(jmsManagerWeblogic) {{

            jmsManagerWeblogic.getMessageDestinationName(sourceQueue);
            result = sourceObjectName;

            jmsManagerWeblogic.deleteMessages(sourceObjectName, anyString);
            result = 1;

            jmsSelectorUtil.getSelector(messageIds);
            result = myselector;

        }};

        jmsManagerWeblogic.deleteMessages(sourceQueue, messageIds);

        new Verifications() {{
            ObjectName capturedSource = null;
            String capturedSelector = null;

            jmsManagerWeblogic.deleteMessages(capturedSource = withCapture(), capturedSelector = withCapture());
            assertTrue(capturedSource == sourceObjectName);
            assertEquals(capturedSelector, myselector);
        }};
    }

    @Test
    public void testSendMessage(final @Injectable InternalJmsMessage internalJmsMessage,
                                final @Mocked javax.jms.Destination jmsDestination) throws Exception {

        final String destinationQueue = "destinationQueue";

        new Expectations(jmsManagerWeblogic) {{

            jmsManagerWeblogic.lookupDestination(destinationQueue);
            result = jmsDestination;

            new MockUp<InitialContext>() {
                @Mock
                javax.jms.Destination doLookup(String jndi) {
                    return jmsDestination;
                }
            };

        }};

        jmsManagerWeblogic.sendMessage(internalJmsMessage, destinationQueue);

        new Verifications() {{
            InternalJmsMessage capturedInternalJmsMessage = null;
            String capturedDestQueue = null;

            jmsManagerWeblogic.sendMessage(capturedInternalJmsMessage = withCapture(), capturedDestQueue = withCapture());
            assertTrue(capturedInternalJmsMessage == internalJmsMessage);
            assertTrue(capturedDestQueue == destinationQueue);
        }};
    }

    @Test
    public void testConsumeMessage(final @Injectable InternalJMSDestination internalJmsDestination,
                                   final @Injectable ObjectName destination,
                                   final @Injectable InternalJmsMessage internalJmsMessage) throws Exception {

        final String sourceQueue = "eDeliveryJMS@ms2@machineName@DomibusNotifyBackendQueue";
        final String customMessageId = "ID1";
        final String selector = "MESSAGE_ID = '" + customMessageId + "' AND NOTIFICATION_TYPE ='MESSAGE_RECEIVED'";

        new Expectations(jmsManagerWeblogic) {{
            jmsManagerWeblogic.getInternalJMSDestinations(sourceQueue);
            result = internalJmsDestination;

            internalJmsDestination.getProperty("ObjectName");
            result = destination;

            jmsManagerWeblogic.getMessageFromDestinationUsingCustomSelector(withAny(destination), selector);
            result = internalJmsMessage;

            jmsManagerWeblogic.deleteMessages(destination, selector);
            result = 1;
        }};

        jmsManagerWeblogic.consumeMessage(sourceQueue, customMessageId);

        new Verifications() {{
            String capturedMessageId = null;
            String capturedSourceQueue = null;

            jmsManagerWeblogic.consumeMessage(capturedSourceQueue = withCapture(), capturedMessageId = withCapture());
            assertEquals(sourceQueue, capturedSourceQueue);
            assertEquals(customMessageId, capturedMessageId);
        }};
    }

    @Test
    public void testConsumeMessageNOk(final @Injectable InternalJMSDestination internalJmsDestination,
                                      final @Injectable ObjectName destination
    ) throws Exception {

        final String sourceQueue = "eDeliveryJMS@ms2@machineName@DomibusNotifyBackendQueue";
        final String customMessageId = "ID1";
        final String selector = "MESSAGE_ID = '" + customMessageId + "' AND NOTIFICATION_TYPE ='MESSAGE_RECEIVED'";

        new Expectations(jmsManagerWeblogic) {{
            jmsManagerWeblogic.getInternalJMSDestinations(sourceQueue);
            result = internalJmsDestination;

            internalJmsDestination.getProperty("ObjectName");
            result = destination;

            jmsManagerWeblogic.getMessageFromDestinationUsingCustomSelector(withAny(destination), selector);
            result = null;

        }};

        jmsManagerWeblogic.consumeMessage(sourceQueue, customMessageId);

        new Verifications() {{
            String capturedSourceQueue = null;

            jmsManagerWeblogic.consumeMessage(capturedSourceQueue = withCapture(), customMessageId);
            assertEquals(sourceQueue, capturedSourceQueue);

            jmsManagerWeblogic.deleteMessages(destination, selector);
            times = 0;
        }};
    }

    @Test
    public void matchesQueueTest(final @Injectable Map.Entry<String, InternalJMSDestination> entry) {
        String queueName = "DomibusBusinessMessageInQueue";

        new Expectations() {{
            entry.getKey();
            returns(queueName, "non-matching-key");

            entry.getValue().<String>getProperty("Jndi");
            returns(null, queueName);
        }};

        boolean found = jmsManagerWeblogic.matchesQueue(queueName, entry);
        assertEquals(true, found);

        boolean found2 = jmsManagerWeblogic.matchesQueue(queueName, entry);
        assertEquals(false, found2);

        boolean found3 = jmsManagerWeblogic.matchesQueue(queueName, entry);
        assertEquals(true, found3);

    }
}
