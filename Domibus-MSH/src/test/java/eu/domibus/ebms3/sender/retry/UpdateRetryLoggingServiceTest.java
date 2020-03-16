
package eu.domibus.ebms3.sender.retry;

import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.core.message.MessagingDao;
import eu.domibus.core.message.nonrepudiation.RawEnvelopeLogDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.ReceptionAwareness;
import eu.domibus.common.model.configuration.RetryStrategy;
import eu.domibus.core.message.MessageLog;
import eu.domibus.core.message.UserMessageLog;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.ebms3.sender.retry.UpdateRetryLoggingService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;

import static eu.domibus.ebms3.sender.retry.UpdateRetryLoggingService.DELETE_PAYLOAD_ON_SEND_FAILURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class UpdateRetryLoggingServiceTest {

    private static final int RETRY_TIMEOUT_IN_MINUTES = 60;

    private static final int RETRY_COUNT = 4;

    private static final long SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 = 1451602800000L; //This is the reference time returned by System.correctTImeMillis() mock

    private static final long FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016 = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - (60 * 5 * 1000);

    private static final long ONE_HOUR_BEFORE_FIRST_OF_JANUARY_2016 = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - (60 * 60 * 1000);

    @Tested
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    private UserMessageLogDao messageLogDao;

    @Injectable
    private UserMessageLogDefaultService messageLogService;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private UIReplicationSignalService uiReplicationSignalService;

    @Injectable
    private RawEnvelopeLogDao rawEnvelopeLogDao;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    MessageAttemptService messageAttemptService;


    /**
     * Max retries limit reached
     * Timeout limit not reached
     * Notification is enabled
     * Expected result: MessageLogDao#setAsNotified() is called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessagingDao#clearPayloadData() is called
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRetryLogging_maxRetriesReachedNotificationEnabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                                  @Injectable UserMessageLog userMessageLog,
                                                                                                  @Injectable LegConfiguration legConfiguration) throws Exception {
        final String messageId = UUID.randomUUID().toString();

        new Expectations(updateRetryLoggingService) {{
            messageLogDao.findByMessageId(messageId, MSHRole.SENDING);
            result = userMessageLog;

            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = false;

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
            updateRetryLoggingService.getScheduledStartDate(userMessageLog);
        }};


        updateRetryLoggingService.updateRetryLogging(messageId, legConfiguration, MessageStatus.WAITING_FOR_RETRY, null);

        new Verifications() {{
            messageLogDao.update(userMessageLog);
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
        }};
    }



    /**
     * Message was restored
     * NextAttempt is set correctly
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRetryLogging_Restored(@Injectable LegConfiguration legConfiguration,
                                                @Injectable UserMessageLog userMessageLog) throws Exception {
        new SystemMockFirstOfJanuary2016(); //current timestamp

        final String messageId = UUID.randomUUID().toString();


        new Expectations() {{
            messageLogDao.findByMessageId(messageId, MSHRole.SENDING);
            result = userMessageLog;

            userMessageLog.getSendAttempts();
            result = 2;
        }};


        updateRetryLoggingService.updateRetryLogging(messageId, legConfiguration, MessageStatus.WAITING_FOR_RETRY, null);

        new Verifications() {{
            userMessageLog.setSendAttempts(3);
        }};
    }

    @Test
    public void testUpdateMessageLogNextAttemptDateForRestoredMessage(@Injectable LegConfiguration legConfiguration,
                                                                      @Injectable UserMessageLog userMessageLog) {

        new SystemMockFirstOfJanuary2016(); //current timestamp

        final long restoredTime = FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016; //Restored 5 min ago
        Date nextAttempt = new Date(FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016 + (RETRY_TIMEOUT_IN_MINUTES / RETRY_COUNT * 60 * 1000));


        new Expectations() {{
            userMessageLog.getNextAttempt();
            result = restoredTime;

            legConfiguration.getReceptionAwareness().getStrategy().getAlgorithm();
            result = RetryStrategy.CONSTANT.getAlgorithm();

            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = RETRY_TIMEOUT_IN_MINUTES;

            legConfiguration.getReceptionAwareness().getRetryCount();
            result = RETRY_COUNT;
        }};

        updateRetryLoggingService.updateMessageLogNextAttemptDate(legConfiguration, userMessageLog);


        new Verifications() {{
            userMessageLog.setNextAttempt(nextAttempt);
        }};
    }

    /**
     * Max retries limit reached
     * Notification is disabled
     * Clear payload is default (false)
     * Expected result: MessagingDao#clearPayloadData is not called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessageLogDao#setAsNotified() is not called
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRetryLogging_maxRetriesReachedNotificationDisabled_ExpectedMessageStatus_ClearPayloadDisabled(@Injectable UserMessage userMessage,
                                                                                                                        @Injectable UserMessageLog userMessageLog,
                                                                                                                        @Injectable LegConfiguration legConfiguration) throws Exception {
        new SystemMockFirstOfJanuary2016();

        final String messageId = UUID.randomUUID().toString();
        final long receivedTime = FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016; //Received 5 min ago

        new Expectations() {{
            userMessageLog.getSendAttempts();
            result = 2;

            userMessageLog.getSendAttemptsMax();
            result = 3;

//            userMessageLog.getReceived();
//            result = new Date(receivedTime);

            userMessageLog.getNotificationStatus();
            result = NotificationStatus.NOT_REQUIRED;

            userMessageLog.getMessageId();
            result = messageId;

            messageLogDao.findByMessageId(messageId, MSHRole.SENDING);
            result = userMessageLog;
        }};

        updateRetryLoggingService.updatePushedMessageRetryLogging(messageId, legConfiguration, null);

        new Verifications() {{
            messageLogService.setMessageAsSendFailure(userMessage, userMessageLog);
            messageLogDao.setAsNotified(userMessageLog);
            times = 0;
        }};

    }

    /**
     * Max retries limit not reached
     * Timeout limit reached
     * Notification is enabled
     * Expected result: MessagingDao#clearPayloadData is called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessageLogDao#setAsNotified() is called
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRetryLogging_timeoutNotificationEnabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                        @Injectable UserMessageLog userMessageLog,
                                                                                        @Injectable LegConfiguration legConfiguration) throws Exception {
        new SystemMockFirstOfJanuary2016();

        final String messageId = UUID.randomUUID().toString();
        final long received = ONE_HOUR_BEFORE_FIRST_OF_JANUARY_2016; // received one hour ago

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DELETE_PAYLOAD_ON_SEND_FAILURE);
            result = true;

            userMessageLog.getSendAttempts();
            result = 0;

            userMessageLog.getSendAttemptsMax();
            result = 3;

            userMessageLog.getNotificationStatus();
            result = NotificationStatus.REQUIRED;

            userMessageLog.getMessageId();
            result = messageId;

            messageLogDao.findByMessageId(messageId, MSHRole.SENDING);
            result = userMessageLog;
        }};


        updateRetryLoggingService.updatePushedMessageRetryLogging(messageId, legConfiguration, null);


        new Verifications() {{
            messageLogService.setMessageAsSendFailure(userMessage, userMessageLog);
            messagingDao.clearPayloadData(userMessage);
        }};

    }


    /**
     * Max retries limit not reached
     * Timeout limit reached
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRetryLogging_timeoutNotificationDisabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                         @Injectable UserMessageLog userMessageLog,
                                                                                         @Injectable LegConfiguration legConfiguration) throws Exception {
        new SystemMockFirstOfJanuary2016();

        final String messageId = UUID.randomUUID().toString();
        final long received = ONE_HOUR_BEFORE_FIRST_OF_JANUARY_2016; // received one hour ago
        int retryTimeout = 1;

        new Expectations(updateRetryLoggingService) {{
            userMessageLog.getSendAttempts();
            result = 0;

            userMessageLog.getSendAttemptsMax();
            result = 3;

            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = retryTimeout;

            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = received;
        }};


        Assert.assertFalse(updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration));
    }

    @Test
    public void testUpdateRetryLogging_success_ExpectedMessageStatus(@Injectable UserMessageLog userMessageLog,
                                                                     @Injectable LegConfiguration legConfiguration,
                                                                     @Injectable MessageAttempt messageAttempt) throws Exception {

        final String messageId = UUID.randomUUID().toString();
        new Expectations(updateRetryLoggingService) {{
            messageLogDao.findByMessageId(messageId, MSHRole.SENDING);
            result = userMessageLog;

            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = true;

            userMessageLog.isTestMessage();
            result = false;

            updateRetryLoggingService.updateNextAttemptAndNotify(legConfiguration, MessageStatus.WAITING_FOR_RETRY, userMessageLog);
        }};

        updateRetryLoggingService.updateRetryLogging(messageId, legConfiguration, MessageStatus.WAITING_FOR_RETRY, messageAttempt);

        new Verifications() {{
            messageLogDao.update(userMessageLog);
            updateRetryLoggingService.updateNextAttemptAndNotify(legConfiguration, MessageStatus.WAITING_FOR_RETRY, userMessageLog);
            messageAttemptService.createAndUpdateEndDate(messageAttempt);
        }};
    }

    @Test
    public void testMessageExpirationDate(@Injectable final MessageLog userMessageLog,
                                          @Injectable final LegConfiguration legConfiguration,
                                          @Injectable ReceptionAwareness receptionAwareness) throws InterruptedException {
        final int timeOut = 10;
        final long timeOutInMillis = 60000 * timeOut;
        final long restoredTime = System.currentTimeMillis();// - (timeOutInMillis + delay);
        final Date expectedDate = new Date(restoredTime + timeOutInMillis);


        new Expectations(updateRetryLoggingService) {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = timeOut;

            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = restoredTime;
        }};

        Date messageExpirationDate = updateRetryLoggingService.getMessageExpirationDate(userMessageLog, legConfiguration);

        assertEquals(expectedDate, messageExpirationDate);
    }

    @Test
    public void testIsExpired(@Injectable final MessageLog userMessageLog,
                              @Injectable final LegConfiguration legConfiguration,
                              @Injectable ReceptionAwareness receptionAwareness) throws InterruptedException {

        int delay = 10;

        final int timeOut = 10;
        final long timeOutInMillis = 60000 * timeOut;
        final long restoredTime = System.currentTimeMillis();// - (timeOutInMillis + delay);
        final Date expectedDate = new Date(restoredTime + timeOutInMillis);


        new Expectations(updateRetryLoggingService) {{
            domibusPropertyProvider.getIntegerProperty(UpdateRetryLoggingService.MESSAGE_EXPIRATION_DELAY);
            result = delay;

            updateRetryLoggingService.getMessageExpirationDate(userMessageLog, legConfiguration);
            result = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - delay - 100;

        }};

        boolean result = updateRetryLoggingService.isExpired(legConfiguration, userMessageLog);
        assertTrue(result);
    }

    private static class SystemMockFirstOfJanuary2016 extends MockUp<System> {
        @Mock
        public static long currentTimeMillis() {
            return SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016;
        }
    }
}