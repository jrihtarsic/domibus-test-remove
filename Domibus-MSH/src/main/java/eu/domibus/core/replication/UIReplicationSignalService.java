package eu.domibus.core.replication;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JMSMessageBuilder;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jms.Queue;

/**
 * Signals creation or update of a User or Signal message
 *
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class UIReplicationSignalService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UIReplicationSignalService.class);

    @Autowired
    @Qualifier("uiReplicationQueue")
    private Queue uiReplicationQueue;

    @Autowired
    protected JMSManager jmsManager;

    @Autowired
    private DomainContextProvider domainContextProvider;

    public void userMessageReceived(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.USER_MESSAGE_RECEIVED.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void messageStatusChange(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.MESSAGE_STATUS_CHANGE.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void messageChange(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.MESSAGE_CHANGE.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void messageNotificationStatusChange(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.MESSAGE_NOTIFICATION_STATUS_CHANGE.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void userMessageSubmitted(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.USER_MESSAGE_SUBMITTED.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                        .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void signalMessageSubmitted(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.SIGNAL_MESSAGE_SUBMITTED.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

    public void signalMessageReceived(String messageId) {
        final JmsMessage message = JMSMessageBuilder.create()
                .type(UIJMSType.SIGNAL_MESSAGE_RECEIVED.name())
                .property(MessageConstants.MESSAGE_ID, messageId)
                .property(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode())
                .build();

        jmsManager.sendMapMessageToQueue(message, uiReplicationQueue);
    }

}
