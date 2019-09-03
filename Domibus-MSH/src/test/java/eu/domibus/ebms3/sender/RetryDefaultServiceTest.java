
package eu.domibus.ebms3.sender;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.core.pull.MessagingLockDao;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.ebms3.common.model.MessageInfo;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.Queue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class RetryDefaultServiceTest {

    private static List<String> QUEUED_MESSAGEIDS = Arrays.asList("queued123@domibus.eu", "queued456@domibus.eu", "queued789@domibus.eu");
    private static List<String> RETRY_MESSAGEIDS = Arrays.asList("retry123@domibus.eu", "retry456@domibus.eu", "queued456@domibus.eu", "expired123@domibus.eu");

    @Tested
    private RetryDefaultService retryService;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private Queue sendMessageQueue;

    @Injectable
    private Queue sendLargeMessageQueue;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private UserMessageLogService userMessageLogService;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private PullMessageService pullMessageService;

    @Injectable
    private JMSManager jmsManager;

    @Injectable
    private RawEnvelopeLogDao rawEnvelopeLogDao;

    @Injectable
    private MessagingLockDao messagingLockDao;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    UpdateRetryLoggingService updateRetryLoggingService;

    private List<JmsMessage> getQueuedMessages() {
        List<JmsMessage> jmsMessages = new ArrayList<>();
        for (String messageId : QUEUED_MESSAGEIDS) {
            JmsMessage jmsMessage = new JmsMessage();
            jmsMessage.setProperty(MessageConstants.MESSAGE_ID, messageId);
            jmsMessages.add(jmsMessage);
        }
        return jmsMessages;
    }

    @Test
    public void getMessagesNotAlreadyQueuedWithNoAlreadyQueuedMessagesTest() {
        List<String> retryMessageIds = Arrays.asList("retry123@domibus.eu", "retry456@domibus.eu", "expired123@domibus.eu");

        new NonStrictExpectations(retryService) {{
            userMessageLogDao.findRetryMessages();
            result = new ArrayList<>(retryMessageIds);
        }};

        List<String> result = retryService.getMessagesNotAlreadyScheduled();
        assertEquals(3, result.size());

        assertEquals(result, retryMessageIds);
    }

    @Test
    public void failIfExpiredTest(@Injectable UserMessage userMessage) throws EbMS3Exception {
        new Expectations() {{
            userMessageLogDao.findRetryMessages();
            result = new ArrayList<>(RETRY_MESSAGEIDS);
        }};

        List<String> messagesNotAlreadyQueued = retryService.getMessagesNotAlreadyScheduled();

        final UserMessageLog userMessageLog = new UserMessageLog();
        userMessageLog.setSendAttempts(2);
        userMessageLog.setSendAttemptsMax(3);
        userMessageLog.setMessageStatus(MessageStatus.WAITING_FOR_RETRY);

        final UserMessage userMessage1 = createUserMessage("expired123@domibus.eu");
        final UserMessage userMessage2 = createUserMessage("retry123@domibus.eu");

        new NonStrictExpectations() {{
            userMessageLogDao.findByMessageId("expired123@domibus.eu", MSHRole.SENDING);
            result = userMessageLog;
            updateRetryLoggingService.isExpired((LegConfiguration) any, userMessageLog);
            result = true;
            updateRetryLoggingService.isExpired((LegConfiguration) any, (UserMessageLog) any);
            result = false;
        }};
        assertTrue(retryService.failIfExpired(userMessage1));
        assertFalse(retryService.failIfExpired(userMessage2));

        for (String messageId : messagesNotAlreadyQueued) {
            UserMessage userMessage3 =  messagingDao.findUserMessageByMessageId(messageId);
            retryService.failIfExpired(userMessage3);
        }
        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
            times = 2; // one outside for and one in for
        }};
    }

    private UserMessage createUserMessage(final String messageId) {
        final MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessageId(messageId);
        final UserMessage userMessage = new UserMessage();
        userMessage.setMessageInfo(messageInfo);
        return userMessage;
    }
}