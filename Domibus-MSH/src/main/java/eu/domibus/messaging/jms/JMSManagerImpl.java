package eu.domibus.messaging.jms;

import eu.domibus.api.jms.JMSDestination;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.jms.spi.InternalJMSDestination;
import eu.domibus.jms.spi.InternalJMSManager;
import eu.domibus.jms.spi.InternalJmsMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@Component
@Transactional
public class JMSManagerImpl implements JMSManager {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(JMSManagerImpl.class);

    @Autowired
    InternalJMSManager internalJmsManager;

    @Autowired
    JMSDestinationMapper jmsDestinationMapper;

    @Autowired
    JMSMessageMapper jmsMessageMapper;

    @Override
    public Map<String, JMSDestination> getDestinations() {
        return jmsDestinationMapper.convert(internalJmsManager.findDestinationsGroupedByFQName());
    }

    @Override
    public JmsMessage getMessage(String source, String messageId) {
        InternalJmsMessage internalJmsMessage = internalJmsManager.getMessage(source, messageId);
        return jmsMessageMapper.convert(internalJmsMessage);
    }

    @Override
    public List<JmsMessage> browseMessages(String source, String jmsType, Date fromDate, Date toDate, String selector) {
        List<InternalJmsMessage> messagesSPI = internalJmsManager.browseMessages(source, jmsType, fromDate, toDate, selector);
        return jmsMessageMapper.convert(messagesSPI);
    }

    @Override
    public List<JmsMessage> browseMessages(String source) {
        List<InternalJmsMessage> messagesSPI = internalJmsManager.browseMessages(source);
        return jmsMessageMapper.convert(messagesSPI);
    }

    @Override
    public void sendMessageToQueue(JmsMessage message, String destination) {
        message.getProperties().put(JmsMessage.PROPERTY_ORIGINAL_QUEUE, destination);
        InternalJmsMessage internalJmsMessage = jmsMessageMapper.convert(message);
        internalJmsManager.sendMessage(internalJmsMessage, destination);
    }

    @Override
    public void sendMessageToQueue(JmsMessage message, Queue destination) {
        try {
            message.getProperties().put(JmsMessage.PROPERTY_ORIGINAL_QUEUE, destination.getQueueName());
        } catch (JMSException e) {
            LOG.warn("Could not add the property [" + JmsMessage.PROPERTY_ORIGINAL_QUEUE + "] on the destination", e);
        }
        InternalJmsMessage internalJmsMessage = jmsMessageMapper.convert(message);
        internalJmsManager.sendMessage(internalJmsMessage, destination);
    }

    @Override
    public void deleteMessages(String source, String[] messageIds) {
        internalJmsManager.deleteMessages(source, messageIds);
    }

    @Override
    public void moveMessages(String source, String destination, String[] messageIds) {
        internalJmsManager.moveMessages(source, destination, messageIds);
    }

    @Override
    public JmsMessage consumeMessage(String source, String messageId) {
        InternalJmsMessage internalJmsMessage = internalJmsManager.consumeMessage(source, messageId);
        return jmsMessageMapper.convert(internalJmsMessage);
    }

    @Override
    public long getDestinationSize(final String destinationName) {
        final Map<String, InternalJMSDestination> destinationsGroupedByFQName = internalJmsManager.findDestinationsGroupedByFQName();
        for (Map.Entry<String, InternalJMSDestination> entry : destinationsGroupedByFQName.entrySet()) {
            if (StringUtils.containsIgnoreCase(entry.getKey(), destinationName)) {
                final InternalJMSDestination value = entry.getValue();
                return value.getNumberOfMessages();
            }
        }
        return 0;
    }
}
