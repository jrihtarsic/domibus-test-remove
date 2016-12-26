package eu.domibus.ebms3.sender;

import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UpdateRetryLoggingService {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(UpdateRetryLoggingService.class);

    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private MessagingDao messagingDao;

    /**
     * This method is responsible for the handling of retries for a given message
     *
     * @param messageId        id of the message that needs to be retried
     * @param legConfiguration processing information for the message
     */
    public void updateRetryLogging(final String messageId, final LegConfiguration legConfiguration) {
        LOGGER.debug("Updating retry for message");
        final MessageLog userMessageLog = this.userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING);
        //userMessageLog.setMessageStatus(MessageStatus.SEND_ATTEMPT_FAILED); //This is not stored in the database
        if (userMessageLog.getSendAttempts() < userMessageLog.getSendAttemptsMax() //handle that there are attempts left
                && (userMessageLog.getReceived().getTime() + legConfiguration.getReceptionAwareness().getRetryTimeout() * 60000) > System.currentTimeMillis()) {// chek that there is time left
            userMessageLog.setSendAttempts(userMessageLog.getSendAttempts() + 1);
            LOGGER.debug("Updating send attempts to [{}]", userMessageLog.getSendAttempts());
            if (legConfiguration.getReceptionAwareness() != null) {
                userMessageLog.setNextAttempt(legConfiguration.getReceptionAwareness().getStrategy().getAlgorithm().compute(userMessageLog.getNextAttempt(), userMessageLog.getSendAttemptsMax(), legConfiguration.getReceptionAwareness().getRetryTimeout()));
                userMessageLog.setMessageStatus(MessageStatus.WAITING_FOR_RETRY);
                LOGGER.debug("Updatig status to [{}]", userMessageLog.getMessageStatus());
                userMessageLogDao.update(userMessageLog);
            }

        } else { // mark message as ultimately failed if max retries reached
            LOGGER.businessError(DomibusMessageCode.BUS_MESSAGE_SEND_FAILURE);
            if (NotificationStatus.REQUIRED.equals(userMessageLog.getNotificationStatus())) {
                LOGGER.debug("Notifying backend for message failure");
                backendNotificationService.notifyOfSendFailure(messageId);
                messagingDao.delete(messageId, MessageStatus.SEND_FAILURE, NotificationStatus.NOTIFIED);
            } else {
                LOGGER.debug("Notifying backend not required for message failure");
                messagingDao.clearPayloadData(messageId);
                userMessageLogDao.setMessageAsSendFailure(messageId);
            }
        }
    }
}
