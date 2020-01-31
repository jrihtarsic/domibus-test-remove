package eu.domibus.core.message;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.message.UserMessageException;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pmode.PModeService;
import eu.domibus.api.pmode.PModeServiceHelper;
import eu.domibus.api.pmode.domain.LegConfiguration;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.SignalMessageDao;
import eu.domibus.common.dao.SignalMessageLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.message.fragment.MessageGroupDao;
import eu.domibus.core.message.fragment.MessageGroupEntity;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.core.pull.PartyExtractor;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.ebms3.common.UserMessageServiceHelper;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.messaging.DelayedDispatchMessageCreator;
import eu.domibus.messaging.DispatchMessageCreator;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.NotificationListener;
import eu.domibus.plugin.handler.DatabaseMessageHandler;
import eu.domibus.plugin.transformer.impl.UserMessageFactory;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Cosmin Baciu, Soumya
 * @since 3.3
 */
@RunWith(JMockit.class)
public class UserMessageDefaultServiceTest {

    private static final long SYSTEM_DATE = new Date().getTime();

    @Tested
    UserMessageDefaultService userMessageDefaultService;

    @Injectable
    private Queue sendMessageQueue;

    @Injectable
    private Queue sendLargeMessageQueue;

    @Injectable
    private Queue splitAndJoinQueue;

    @Injectable
    private Queue sendPullReceiptQueue;

    @Injectable
    private Queue retentionMessageQueue;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private SignalMessageLogDao signalMessageLogDao;

    @Injectable
    private UserMessageLogDefaultService userMessageLogService;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private UserMessageServiceHelper userMessageServiceHelper;

    @Injectable
    private SignalMessageDao signalMessageDao;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    private JMSManager jmsManager;

    @Injectable
    DomainCoreConverter domainConverter;

    @Injectable
    PModeService pModeService;

    @Injectable
    PModeServiceHelper pModeServiceHelper;

    @Injectable
    MessageExchangeService messageExchangeService;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    private PullMessageService pullMessageService;

    @Injectable
    protected UIReplicationSignalService uiReplicationSignalService;

    @Injectable
    MessageGroupDao messageGroupDao;

    @Injectable
    UserMessageFactory userMessageFactory;

    @Injectable
    DatabaseMessageHandler databaseMessageHandler;

    @Injectable
    PModeProvider pModeProvider;


    @Test
    public void createMessagingForFragment(@Injectable UserMessage sourceMessage,
                                           @Injectable MessageGroupEntity messageGroupEntity,
                                           @Injectable UserMessage userMessageFragment) throws MessagingProcessingException {
        String messageId = "123";
        String backendName = "mybackend";

        List<String> fragmentFiles = new ArrayList<>();
        final String fragment1 = "fragment1";
        fragmentFiles.add(fragment1);

        new Expectations() {{
            userMessageFactory.createUserMessageFragment(sourceMessage, messageGroupEntity, 1L, fragment1);
            result = userMessageFragment;
        }};

        userMessageDefaultService.createMessagingForFragment(sourceMessage, messageGroupEntity, backendName, fragment1, 1);

        new Verifications() {{
            userMessageFactory.createUserMessageFragment(sourceMessage, messageGroupEntity, 1L, fragment1);
            databaseMessageHandler.submitMessageFragment(userMessageFragment, backendName);
        }};
    }

    @Test
    public void createMessageFragments(@Injectable UserMessage sourceMessage,
                                       @Injectable MessageGroupEntity messageGroupEntity
    ) throws MessagingProcessingException {
        String messageId = "123";
        String backendName = "mybackend";

        List<String> fragmentFiles = new ArrayList<>();
        final String fragment1 = "fragment1";
        fragmentFiles.add(fragment1);


        new Expectations(userMessageDefaultService) {{
            sourceMessage.getMessageInfo().getMessageId();
            result = messageId;

            userMessageLogDao.findBackendForMessageId(messageId);
            result = backendName;


            userMessageDefaultService.createMessagingForFragment(sourceMessage, messageGroupEntity, backendName, anyString, anyInt);
        }};

        userMessageDefaultService.createMessageFragments(sourceMessage, messageGroupEntity, fragmentFiles);

        new Verifications() {{
            messageGroupDao.create(messageGroupEntity);

            userMessageDefaultService.createMessagingForFragment(sourceMessage, messageGroupEntity, backendName, fragment1, 1);
        }};
    }

    @Test
    public void testGetFinalRecipient(@Injectable final UserMessage userMessage) throws Exception {
        final String messageId = "1";

        new Expectations() {{
            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

        }};

        userMessageDefaultService.getFinalRecipient(messageId);

        new Verifications() {{
            userMessageServiceHelper.getFinalRecipient(userMessage);
        }};
    }

    @Test
    public void testGetFinalRecipientWhenNoMessageIsFound(@Injectable final UserMessage userMessage) throws Exception {
        final String messageId = "1";

        new Expectations() {{
            messagingDao.findUserMessageByMessageId(messageId);
            result = null;

        }};

        Assert.assertNull(userMessageDefaultService.getFinalRecipient(messageId));
    }

    @Test
    public void testFailedMessages(@Injectable final UserMessage userMessage) throws Exception {
        final String finalRecipient = "C4";

        userMessageDefaultService.getFailedMessages(finalRecipient);

        new Verifications() {{
            userMessageLogDao.findFailedMessages(finalRecipient);
        }};
    }

    @Test
    public void testGetFailedMessageElapsedTime(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";
        final Date failedDate = new Date();

        new CurrentTimeMillisMock();

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            result = userMessageLog;

            userMessageLog.getFailed();
            result = failedDate;
        }};

        final Long failedMessageElapsedTime = userMessageDefaultService.getFailedMessageElapsedTime(messageId);
        Assert.assertTrue(SYSTEM_DATE - failedDate.getTime() == failedMessageElapsedTime);
    }

    @Test(expected = UserMessageException.class)
    public void testGetFailedMessageElapsedTimeWhenFailedDateIsNull(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new CurrentTimeMillisMock();

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            result = userMessageLog;

            userMessageLog.getFailed();
            result = null;
        }};

        userMessageDefaultService.getFailedMessageElapsedTime(messageId);
    }

    @Test(expected = UserMessageException.class)
    public void testRestoreMessageWhenMessageIsDeleted(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            result = userMessageLog;

            userMessageLog.getMessageStatus();
            result = MessageStatus.DELETED;

        }};

        userMessageDefaultService.restoreFailedMessage(messageId);
    }

    @Test
    public void testRestorePushedMessage(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";
        final Integer newMaxAttempts = 5;

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            result = userMessageLog;

            messageExchangeService.retrieveMessageRestoreStatus(messageId);
            result = MessageStatus.SEND_ENQUEUED;

            userMessageDefaultService.computeNewMaxAttempts(userMessageLog, messageId);
            result = newMaxAttempts;

            userMessageLog.getMessageStatus();
            result = MessageStatus.SEND_ENQUEUED;

        }};

        userMessageDefaultService.restoreFailedMessage(messageId);

        new FullVerifications(userMessageDefaultService) {{
            backendNotificationService.notifyOfMessageStatusChange(withAny(new UserMessageLog()), MessageStatus.SEND_ENQUEUED, withAny(new Timestamp(System.currentTimeMillis())));

            userMessageLog.setMessageStatus(MessageStatus.SEND_ENQUEUED);
            userMessageLog.setRestored(withAny(new Date()));
            userMessageLog.setFailed(null);
            userMessageLog.setNextAttempt(withAny(new Date()));
            userMessageLog.setSendAttemptsMax(newMaxAttempts);

            userMessageLogDao.update(userMessageLog);
            uiReplicationSignalService.messageChange(anyString);
            userMessageDefaultService.scheduleSending(userMessageLog);

        }};
    }

    @Test
    public void testRestorePUlledMessage(@Injectable final UserMessageLog userMessageLog, @Injectable final UserMessage userMessage) throws Exception {
        final String messageId = "1";
        final Integer newMaxAttempts = 5;
        final String mpc = "mpc";

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            result = userMessageLog;

            messageExchangeService.retrieveMessageRestoreStatus(messageId);
            result = MessageStatus.READY_TO_PULL;

            userMessageDefaultService.computeNewMaxAttempts(userMessageLog, messageId);
            result = newMaxAttempts;

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

        }};

        userMessageDefaultService.restoreFailedMessage(messageId);

        new Verifications() {{
            userMessageLog.setMessageStatus(MessageStatus.READY_TO_PULL);
            times = 1;
            userMessageLog.setRestored(withAny(new Date()));
            times = 1;
            userMessageLog.setFailed(null);
            times = 1;
            userMessageLog.setNextAttempt(withAny(new Date()));
            times = 1;
            userMessageLog.setSendAttemptsMax(newMaxAttempts);
            times = 1;

            userMessageLogDao.update(userMessageLog);
            times = 1;
            userMessageDefaultService.scheduleSending(messageId, anyBoolean);
            times = 0;
            messagingDao.findUserMessageByMessageId(messageId);
            times = 1;
            PartyExtractor partyExtractor = null;
            pullMessageService.addPullMessageLock(withAny(partyExtractor), userMessage, userMessageLog);
            times = 1;
        }};
    }

    @Test
    public void testMaxAttemptsConfigurationWhenNoLegIsFound() throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            pModeService.getLegConfiguration(messageId);
            result = null;

        }};

        final Integer maxAttemptsConfiguration = userMessageDefaultService.getMaxAttemptsConfiguration(messageId);
        Assert.assertTrue(maxAttemptsConfiguration == 1);

    }

    @Test
    public void testMaxAttemptsConfiguration(@Injectable final LegConfiguration legConfiguration) throws Exception {
        final String messageId = "1";
        final Integer pModeMaxAttempts = 5;

        new Expectations(userMessageDefaultService) {{
            pModeService.getLegConfiguration(messageId);
            result = legConfiguration;

            pModeServiceHelper.getMaxAttempts(legConfiguration);
            result = pModeMaxAttempts;

        }};

        final Integer maxAttemptsConfiguration = userMessageDefaultService.getMaxAttemptsConfiguration(messageId);
        Assert.assertTrue(maxAttemptsConfiguration == pModeMaxAttempts);
    }

    @Test
    public void testComputeMaxAttempts(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";
        final Integer pModeMaxAttempts = 5;

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getMaxAttemptsConfiguration(messageId);
            result = pModeMaxAttempts;

            userMessageLog.getSendAttemptsMax();
            result = pModeMaxAttempts;

        }};

        final Integer maxAttemptsConfiguration = userMessageDefaultService.computeNewMaxAttempts(userMessageLog, messageId);
        Assert.assertTrue(maxAttemptsConfiguration == 11);
    }

    @Test
    public void testScheduleSending(@Injectable final JmsMessage jmsMessage,
                                    @Mocked DispatchMessageCreator dispatchMessageCreator,
                                    @Injectable UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageIdSafely(messageId);
            result = userMessageLog;

            userMessageLog.getMessageId();
            result = messageId;

            new DispatchMessageCreator(messageId);
            result = dispatchMessageCreator;

            dispatchMessageCreator.createMessage();
            result = jmsMessage;

        }};

        userMessageDefaultService.scheduleSending(messageId, false);

        new Verifications() {{
            jmsManager.sendMessageToQueue(jmsMessage, sendMessageQueue);
        }};

    }

    @Test
    public void testSchedulePullReceiptSending(@Injectable final JmsMessage jmsMessage) throws Exception {
        final String messageId = "1";
        final String pModeKey = "pModeKey";


        userMessageDefaultService.scheduleSendingPullReceipt(messageId, pModeKey);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, sendPullReceiptQueue);
        }};

    }

    @Test
    public void testRestoreFailedMessagesDuringPeriodWhenAPreviousMessageIsFailing() throws Exception {
        final String finalRecipient = "C4";
        final Date startDate = new Date();
        final Date endDate = new Date();

        final String failedMessage1 = "1";
        final String failedMessage2 = "2";
        final List<String> failedMessages = new ArrayList<>();
        failedMessages.add(failedMessage1);
        failedMessages.add(failedMessage2);

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findFailedMessages(finalRecipient, startDate, endDate);
            result = failedMessages;

            userMessageDefaultService.restoreFailedMessage(failedMessage1);

            userMessageDefaultService.restoreFailedMessage(failedMessage2);
            result = new RuntimeException("Problem restoring message 2");
        }};

        final List<String> restoredMessages = userMessageDefaultService.restoreFailedMessagesDuringPeriod(startDate, endDate, finalRecipient);
        assertNotNull(restoredMessages);
        assertEquals(1, restoredMessages.size());
        assertEquals(failedMessage1, restoredMessages.iterator().next());
    }

    @Test
    public void testRestoreFailedMessagesDuringPeriod() throws Exception {
        final String finalRecipient = "C4";
        final Date startDate = new Date();
        final Date endDate = new Date();

        final String failedMessage1 = "1";
        final String failedMessage2 = "2";
        final List<String> failedMessages = new ArrayList<>();
        failedMessages.add(failedMessage1);
        failedMessages.add(failedMessage2);

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findFailedMessages(finalRecipient, startDate, endDate);
            result = failedMessages;

            userMessageDefaultService.restoreFailedMessage(anyString);
        }};

        final List<String> restoredMessages = userMessageDefaultService.restoreFailedMessagesDuringPeriod(startDate, endDate, finalRecipient);
        assertNotNull(restoredMessages);
        assertEquals(restoredMessages, failedMessages);
    }

    @Test(expected = UserMessageException.class)
    public void testFailedMessageWhenNoMessageIsFound(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = null;
        }};

        userMessageDefaultService.getFailedMessage(messageId);
    }

    @Test(expected = UserMessageException.class)
    public void testFailedMessageWhenStatusIsNotFailed(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            userMessageLog.getMessageStatus();
            result = MessageStatus.RECEIVED;
        }};

        userMessageDefaultService.getFailedMessage(messageId);
    }

    @Test
    public void testGetFailedMessage(@Injectable final UserMessageLog userMessageLog) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            userMessageLog.getMessageStatus();
            result = MessageStatus.SEND_FAILURE;
        }};

        final UserMessageLog failedMessage = userMessageDefaultService.getFailedMessage(messageId);
        Assert.assertNotNull(failedMessage);
    }

    @Test
    public void testDeleteMessaged(@Mocked final NotificationListener notificationListener1) throws Exception {
        final String messageId = "1";
        final String queueName = "wsQueue";

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.deleteMessagePluginCallback((String) any);
        }};

        userMessageDefaultService.deleteMessage(messageId);

        new Verifications() {{
            userMessageDefaultService.deleteMessagePluginCallback(messageId);
        }};
    }

    @Test
    public void testDeleteMessagePluginCallback(@Injectable final NotificationListener notificationListener1,
                                                @Injectable UserMessageLog userMessageLog) {
        final String messageId = "1";
        final String backend = "myPlugin";
        final List<NotificationListener> notificationListeners = new ArrayList<>();
        notificationListeners.add(notificationListener1);

        new Expectations(userMessageDefaultService) {{
            backendNotificationService.getNotificationListenerServices();
            result = notificationListeners;

            userMessageLogDao.findByMessageIdSafely(messageId);
            result = userMessageLog;

            userMessageLog.getBackend();
            result = backend;

            backendNotificationService.getNotificationListener(backend);
            result = notificationListener1;

            userMessageDefaultService.deleteMessagePluginCallback((String) any, (NotificationListener) any);
        }};

        userMessageDefaultService.deleteMessagePluginCallback(messageId);

        new Verifications() {{
            userMessageDefaultService.deleteMessagePluginCallback(messageId, notificationListener1);
        }};
    }

    @Test
    public void testDeleteMessagePluginCallbackForNotificationListener(@Injectable final NotificationListener notificationListener,
                                                                       @Injectable UserMessageLog userMessageLog,
                                                                       @Injectable Queue backendNotificationQueue) throws JMSException {
        final String messageId = "1";
        final String backendQueue = "myPluginQueue";

        new Expectations(userMessageDefaultService) {{
            notificationListener.getBackendNotificationQueue();
            result = backendNotificationQueue;

            backendNotificationQueue.getQueueName();
            result = backendQueue;


        }};

        userMessageDefaultService.deleteMessagePluginCallback(messageId, notificationListener);

        new Verifications() {{
            jmsManager.consumeMessage(backendQueue, messageId);
            notificationListener.deleteMessageCallback(messageId);
        }};
    }

    @Test
    public void marksTheUserMessageAsDeleted(@Injectable Messaging messaging,
                                             @Injectable UserMessage userMessage,
                                             @Injectable UserMessageLog userMessageLog,
                                             @Injectable SignalMessage signalMessage) throws Exception {
        final String messageId = "1";

        new Expectations(userMessageDefaultService) {{
            messagingDao.findMessageByMessageId(messageId);
            result = messaging;

            messaging.getUserMessage();
            result = userMessage;

            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            messaging.getSignalMessage();
            result = signalMessage;
        }};

        userMessageDefaultService.deleteMessage(messageId);

        new Verifications() {{
            messagingDao.clearPayloadData(userMessage);
            userMessageLogService.setMessageAsDeleted(userMessage, userMessageLog);
            userMessageLogService.setSignalMessageAsDeleted(signalMessage.getMessageInfo().getMessageId());
        }};
    }

    @Test
    public void test_ResendFailedOrSendEnqueuedMessage_StatusSendEnqueued(final @Mocked UserMessageLog userMessageLog) {
        final String messageId = UUID.randomUUID().toString();

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            userMessageLog.getMessageStatus();
            result = MessageStatus.SEND_ENQUEUED;
        }};

        //tested method
        userMessageDefaultService.resendFailedOrSendEnqueuedMessage(messageId);

        new FullVerifications(userMessageDefaultService) {{
            String messageIdActual;
            userMessageDefaultService.sendEnqueuedMessage(messageIdActual = withCapture());
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void test_ResendFailedOrSendEnqueuedMessage_StatusFailed(final @Mocked UserMessageLog userMessageLog) {
        final String messageId = UUID.randomUUID().toString();

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            userMessageLog.getMessageStatus();
            result = MessageStatus.SEND_FAILURE;
        }};

        //tested method
        userMessageDefaultService.resendFailedOrSendEnqueuedMessage(messageId);

        new FullVerifications(userMessageDefaultService) {{
            String messageIdActual;
            userMessageDefaultService.restoreFailedMessage(messageIdActual = withCapture());
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void test_ResendFailedOrSendEnqueuedMessage_MessageNotFound(final @Mocked UserMessageLog userMessageLog) {
        final String messageId = UUID.randomUUID().toString();

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = null;
        }};

        try {
            //tested method
            userMessageDefaultService.resendFailedOrSendEnqueuedMessage(messageId);
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertEquals(UserMessageException.class, e.getClass());
        }

        new FullVerifications(userMessageDefaultService) {{
        }};
    }

    private static class CurrentTimeMillisMock extends MockUp<System> {
        @Mock
        public static long currentTimeMillis() {
            return SYSTEM_DATE;
        }
    }

    @Test
    public void deleteFailedMessageTest() {
        final String messageId = UUID.randomUUID().toString();

        new Expectations(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            times = 1;
            userMessageDefaultService.deleteMessage(messageId);
            times = 1;
        }};

        userMessageDefaultService.deleteFailedMessage(messageId);

        new FullVerificationsInOrder(userMessageDefaultService) {{
            userMessageDefaultService.getFailedMessage(messageId);
            userMessageDefaultService.deleteMessage(messageId);
        }};
    }

    @Test
    public void scheduleSendingWithDelayTest(@Injectable final JmsMessage jmsMessage,
                                             @Mocked DelayedDispatchMessageCreator delayedDispatchMessageCreator,
                                             @Injectable UserMessageLog userMessageLog) {
        final String messageId = UUID.randomUUID().toString();
        Long delay = 1L;
        boolean isSplitAndJoin = false;

        new Expectations(userMessageDefaultService) {{
            userMessageLogDao.findByMessageIdSafely(messageId);
            result = userMessageLog;

            new DelayedDispatchMessageCreator(messageId, delay);
            result = delayedDispatchMessageCreator;

            delayedDispatchMessageCreator.createMessage();
            result = jmsMessage;

        }};

        userMessageDefaultService.scheduleSending(messageId, delay, isSplitAndJoin);

        new Verifications() {{
            userMessageDefaultService.scheduleSending(userMessageLog, new DelayedDispatchMessageCreator(messageId, delay).createMessage());
            times = 1;
        }};
    }

    @Test
    public void scheduleSendingWithRetryCountTest(@Injectable final JmsMessage jmsMessage,
                                                  @Mocked DispatchMessageCreator dispatchMessageCreator) {
        final String messageId = UUID.randomUUID().toString();
        ;
        int retryCount = 3;
        boolean isSplitAndJoin = false;

        new Expectations(userMessageDefaultService) {{
            new DispatchMessageCreator(messageId);
            result = dispatchMessageCreator;

            dispatchMessageCreator.createMessage(retryCount);
            result = jmsMessage;

        }};

        userMessageDefaultService.scheduleSending(messageId, retryCount, isSplitAndJoin);

        new Verifications() {{
            userMessageDefaultService.scheduleSending(messageId, null, new DispatchMessageCreator(messageId).createMessage(retryCount), isSplitAndJoin);
            times = 1;
        }};
    }

    @Test
    public void scheduleSendingPullReceiptWithRetryCountTest(@Injectable final JmsMessage jmsMessage,
                                                             @Injectable UserMessageService userMessageService) {
        final String messageId = UUID.randomUUID().toString();
        final String pModeKey = "pModeKey";
        final int retryCount = 3;

        userMessageDefaultService.scheduleSendingPullReceipt(messageId, pModeKey, retryCount);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, sendPullReceiptQueue);
        }};
    }

    @Test
    public void scheduleSplitAndJoinReceiveFailedTest(@Injectable final JmsMessage jmsMessage,
                                                      @Injectable UserMessageService userMessageService) {
        final String sourceMessageId = UUID.randomUUID().toString();
        final String groupId = "groupId";
        final String errorCode = "ebms3ErrorCode";
        final String errorDetail = "ebms3ErrorDetail";

        userMessageDefaultService.scheduleSplitAndJoinReceiveFailed(groupId, sourceMessageId, errorCode, errorDetail);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};

    }

    @Test
    public void scheduleSendingSignalErrorTest(@Injectable final JmsMessage jmsMessage,
                                               @Injectable UserMessageService userMessageService) {
        final String messageId = UUID.randomUUID().toString();
        final String pmodeKey = "pmodeKey";
        final String ebMS3ErrorCode = "ebms3ErrorCode";
        final String errorDetail = "ebms3ErrorDetail";

        userMessageDefaultService.scheduleSendingSignalError(messageId, ebMS3ErrorCode, errorDetail, pmodeKey);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSourceMessageReceiptTest(@Injectable final JmsMessage jmsMessage,
                                                 @Injectable UserMessageService userMessageService) {
        final String messageId = UUID.randomUUID().toString();
        final String pmodeKey = "pmodeKey";

        userMessageDefaultService.scheduleSourceMessageReceipt(messageId, pmodeKey);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSourceMessageRejoinTest(@Injectable final JmsMessage jmsMessage,
                                                @Injectable UserMessageService userMessageService) {
        final String groupId = "groupId";
        final String file = "SourceMessageFile";
        final String backendName = "backendName";

        userMessageDefaultService.scheduleSourceMessageRejoin(groupId, file, backendName);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSourceMessageRejoinFileTest(@Injectable final JmsMessage jmsMessage,
                                                    @Injectable UserMessageService userMessageService) {
        final String groupId = "groupId";
        final String backendName = "backendName";

        userMessageDefaultService.scheduleSourceMessageRejoinFile(groupId, backendName);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSetUserMessageFragmentAsFailedTest(@Injectable final JmsMessage jmsMessage,
                                                           @Injectable UserMessageService userMessageService) {
        final String messageId = UUID.randomUUID().toString();

        userMessageDefaultService.scheduleSetUserMessageFragmentAsFailed(messageId);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSplitAndJoinSendFailedTest(@Injectable final JmsMessage jmsMessage,
                                                   @Injectable UserMessageService userMessageService) {
        final String groupId = "groupId";
        final String errorDetail = "ebms3ErrorDetail";

        userMessageDefaultService.scheduleSplitAndJoinSendFailed(groupId, errorDetail);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, splitAndJoinQueue);
        }};
    }

    @Test
    public void scheduleSourceMessageSendingTest(@Injectable final JmsMessage jmsMessage,
                                                 @Mocked DispatchMessageCreator dispatchMessageCreator) {
        final String messageId = UUID.randomUUID().toString();

        new Expectations() {{
            new DispatchMessageCreator(messageId);
            result = dispatchMessageCreator;

            dispatchMessageCreator.createMessage();
            result = jmsMessage;
        }};

        userMessageDefaultService.scheduleSourceMessageSending(messageId);

        new Verifications() {{
            jmsManager.sendMessageToQueue((JmsMessage) any, sendLargeMessageQueue);
        }};
    }
}
