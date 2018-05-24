package eu.domibus.ebms3.sender;

import eu.domibus.api.jms.DomibusJMSException;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.core.pull.MessagingLockDao;
import eu.domibus.core.pull.PullLockAckquire;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.ebms3.common.model.MessagingLock;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Service
public class RetryService {
    public static final String TIMEOUT_TOLERANCE = "domibus.msh.retry.tolerance";
    private static final String DELETE_PAYLOAD_ON_SEND_FAILURE = "domibus.sendMessage.failure.delete.payload";
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(RetryService.class);
    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Autowired
    @Qualifier("jmsTemplateDispatch")
    private JmsOperations jmsOperations;

    @Autowired
    @Qualifier("sendMessageQueue")
    private Queue dispatchQueue;

    @Autowired
    UserMessageService userMessageService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private UserMessageLogService userMessageLogService;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private PullMessageService pullMessageService;

    @Autowired
    private JMSManager jmsManager;

    @Autowired
    private RawEnvelopeLogDao rawEnvelopeLogDao;

    @Autowired
    private MessagingLockDao messagingLockDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueMessages() {
        final List<String> messageIdsToPurge = userMessageLogDao.findTimedoutMessages(Integer.parseInt(domibusProperties.getProperty(RetryService.TIMEOUT_TOLERANCE)));
        for (final String messageIdToPurge : messageIdsToPurge) {
            purgeTimedoutMessage(messageIdToPurge);
        }
        LOG.debug(messageIdsToPurge.size() + " messages to purge found");

        final List<String> messagesNotAlreadyQueued = getMessagesNotAlreadyQueued();
        for (final String messageId : messagesNotAlreadyQueued) {
            userMessageService.scheduleSending(messageId);
        }
    }

    protected List<String> getMessagesNotAlreadyQueued() {
        List<String> result = new ArrayList<>();

        final List<String> messageIdsToSend = userMessageLogDao.findRetryMessages();
        if (messageIdsToSend.isEmpty()) {
            return result;
        }
        LOG.debug("Messages to be retried [{}]", messageIdsToSend);
        final List<String> queuedMessages = getQueuedMessages();
        messageIdsToSend.removeAll(queuedMessages);
        return messageIdsToSend;
    }

    protected List<String> getQueuedMessages() {
        List<String> result = new ArrayList<>();
        try {
            final List<JmsMessage> jmsMessages = jmsManager.browseMessages(dispatchQueue.getQueueName());
            if (jmsMessages == null) {
                return result;
            }
            for (JmsMessage jmsMessage : jmsMessages) {
                result.add(jmsMessage.getStringProperty(MessageConstants.MESSAGE_ID));
            }
            return result;
        } catch (JMSException e) {
            throw new DomibusJMSException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgePullMessage() {
        List<String> timedoutPullMessages = userMessageLogDao.findTimedOutPullMessages(Integer.parseInt(domibusProperties.getProperty(RetryService.TIMEOUT_TOLERANCE)));
        for (final String timedoutPullMessage : timedoutPullMessages) {
            LOG.debug("[purgePullMessage]:Message:[{}] delete lock ", timedoutPullMessage);
            pullMessageService.deletePullMessageLock(timedoutPullMessage);
            rawEnvelopeLogDao.deleteUserMessageRawEnvelope(timedoutPullMessage);
            purgeTimedoutMessage(timedoutPullMessage);
        }
    }


    /**
     * Notifies send failure, updates the message status and deletes the payload (if required) for messages that failed to be sent and expired
     *
     * @param messageIdToPurge is the messageId of the expired message
     */
    //TODO in Domibus 3.3 extract the logic below into a method of the MessageService and re-use it here and in the UpdateRetryLoggingService
    private void purgeTimedoutMessage(final String messageIdToPurge) {
        final MessageLog userMessageLog = userMessageLogDao.findByMessageId(messageIdToPurge, MSHRole.SENDING);

        final boolean notify = NotificationStatus.REQUIRED.equals(userMessageLog.getNotificationStatus());

        if (notify) {
            backendNotificationService.notifyOfSendFailure(messageIdToPurge);

        }
        userMessageLogService.setMessageAsSendFailure(messageIdToPurge);

        if ("true".equals(domibusProperties.getProperty(DELETE_PAYLOAD_ON_SEND_FAILURE, "false"))) {
            messagingDao.clearPayloadData(messageIdToPurge);
        }
    }

    /**
     * Notifies send failure, updates the message status and deletes the payload (if required) for messages that failed to be sent and expired
     * Note: This method creates a new transaction
     *
     * @param messageIdToPurge is the messageId of the expired message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgeTimedoutMessageInANewTransaction(final String messageIdToPurge) {
        rawEnvelopeLogDao.deleteUserMessageRawEnvelope(messageIdToPurge);
        purgeTimedoutMessage(messageIdToPurge);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public void resetWaitingForReceiptPullMessages() {
        final List<String> messagesToReset = userMessageLogDao.findPullWaitingForReceiptMessages();
        for (String messagedId : messagesToReset) {
            //should be deleted but in order to do so, this process should also lock the messagingLock and delte it
            //to avoid having deadlock when deleting the raw message. Next version.
            //rawEnvelopeLogDao.deleteUserMessageRawEnvelope(messagedId);
            LOG.debug("[PULL]:Message[{}]: reset into ready to pull.", messagedId);
            LOG.debug("[resetWaitingForReceiptPullMessages]:Message:[{}] delete lock ", messagedId);
            PullLockAckquire lockAcquired = pullMessageService.lockAndDeleteMessageLock(messagedId);
            if (lockAcquired == null) {
                LOG.debug("[PULL]:Message[{}]:could not acquire the lock. Message skipped");
                continue;
            }
            final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messagedId);
            LOG.debug("Message:[{}] has reach its next attempt offset, send attempts[{}]", messagedId, userMessageLog.getSendAttempts());
            if (userMessageLog.getSendAttempts() < userMessageLog.getSendAttemptsMax() && lockAcquired.getExpirationTimeStamp() < System.currentTimeMillis()) {
                LOG.debug("Message:[{}] has reach its next attempt moment[{}].", messagedId, userMessageLog.getNextAttempt());
                pullMessageService.reset(userMessageLog);
                //notify ??
            } else {
                LOG.debug("Message:[{}] has no more attempt.", messagedId);
                final MessageStatus sendFailure = MessageStatus.SEND_FAILURE;
                LOG.debug("Set message:[{}] in state:[{}].", messagedId, sendFailure);
                pullMessageService.sendFailed(userMessageLog);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public void bulkExpirePullMessages() {
        final List<MessagingLock> staledMessages = messagingLockDao.findStaledMessages();
        LOG.trace("Delete expired pull message");
        for (MessagingLock staledMessage : staledMessages) {
            final String messageId = staledMessage.getMessageId();
            LOG.debug("[bulkExpirePullMessages]:Message:[{}] delete lock ", messageId);
            PullLockAckquire lockAndDelete = pullMessageService.lockAndDeleteMessageLock(messageId);
            if (lockAndDelete == null) {
                continue;
            }
            LOG.debug("Message:[{}] expired.", messageId);
            pullMessageService.sendFailed(userMessageLogDao.findByMessageId(messageId));
        }
    }

}
