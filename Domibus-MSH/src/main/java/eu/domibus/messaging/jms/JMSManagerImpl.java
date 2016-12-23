package eu.domibus.messaging.jms;

import eu.domibus.api.jms.JMSDestination;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.jms.spi.InternalJMSDestination;
import eu.domibus.jms.spi.InternalJMSManager;
import eu.domibus.jms.spi.InternalJmsMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log LOG = LogFactory.getLog(JMSManagerImpl.class);

    @Autowired
    InternalJMSManager internalJmsManager;

    @Autowired
    JMSDestinationMapper jmsDestinationMapper;

    @Autowired
    JMSMessageMapper jmsMessageMapper;

    @Override
    public Map<String, JMSDestination> getDestinations() {
        Map<String, InternalJMSDestination> destinations = internalJmsManager.getDestinations();
        return jmsDestinationMapper.convert(destinations);
    }

    @Override
    public JmsMessage getMessage(String source, String messageId) {
        InternalJmsMessage internalJmsMessage = internalJmsManager.getMessage(source, messageId);
        return jmsMessageMapper.convert(internalJmsMessage);
    }

    @Override
    public List<JmsMessage> getMessages(String source, String jmsType, Date fromDate, Date toDate, String selector) {
        List<InternalJmsMessage> messagesSPI = internalJmsManager.getMessages(source, jmsType, fromDate, toDate, selector);
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
}
