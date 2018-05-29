package eu.domibus.core.pull;

import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.ebms3.common.model.MessagingLock;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.ebms3.sender.UpdateRetryLoggingService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class PullMessageServiceImplTest {

    @Injectable
    private UserMessageLogService userMessageLogService;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private PullMessageStateService pullMessageStateService;

    @Injectable
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private RawEnvelopeLogDao rawEnvelopeLogDao;

    @Injectable
    private MessagingLockDao messagingLockDao;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private java.util.Properties domibusProperties;

    @Injectable
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Tested
    private PullMessageServiceImpl pullMessageService;


    @Test
    public void setDataSource(@Mocked NamedParameterJdbcTemplate namedParameterJdbcTemplate, @Mocked final DataSource dataSource) {
        pullMessageService.setDataSource(dataSource);
        new Verifications() {{
            final List<NamedParameterJdbcTemplate> namedParameterJdbcTemplates = withCapture(new NamedParameterJdbcTemplate(dataSource));
            assertEquals(1, namedParameterJdbcTemplates.size());
        }};
    }

    @Test
    public void updatePullMessageAfterRequest() {
    }

    @Test
    public void hasAttemptsLeft() {
    }

    @Test
    public void updatePullMessageAfterReceipt() {
    }

    @Test
    public void addPullMessageLock() {
    }

    @Test
    public void deletePullMessageLock() {
    }

    @Test
    public void getPullMessageId(@Mocked final SqlRowSet sqlRowSet, @Mocked final Map<String, Object> params) {
        final String initiator = "initiator";
        final String mpc = "mpc";
        new Expectations() {{
            jdbcTemplate.queryForRowSet("", params);
            result = sqlRowSet;
        }};
        pullMessageService.getPullMessageId(initiator, mpc);

    }

    @Test(expected = PModeException.class)
    public void addPullMessageLockWithPmodeException(@Mocked final PartyIdExtractor partyIdExtractor, @Mocked final UserMessage userMessage, @Mocked final MessageLog messageLog) throws EbMS3Exception {
        final String pmodeKey = "pmodeKey";
        final String partyId = "partyId";
        final String messageId = "messageId";
        final String mpc = "mpc";
        final Date staledDate = new Date();
        final LegConfiguration legConfiguration = new LegConfiguration();
        new Expectations(pullMessageService) {{
            partyIdExtractor.getPartyId();
            result = partyId;
            messageLog.getMessageId();
            result = messageId;
            messageLog.getMpc();
            result = mpc;
            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "", "", null);
        }};

        pullMessageService.addPullMessageLock(partyIdExtractor, userMessage, messageLog);
    }

    @Test
    public void addPullMessageLock(@Mocked final PartyIdExtractor partyIdExtractor, @Mocked final UserMessage userMessage, @Mocked final MessageLog messageLog) throws EbMS3Exception {
        final String pmodeKey = "pmodeKey";
        final String partyId = "partyId";
        final String messageId = "messageId";
        final String mpc = "mpc";
        final Date staledDate = new Date();
        final LegConfiguration legConfiguration = new LegConfiguration();
        new Expectations(pullMessageService) {{
            partyIdExtractor.getPartyId();
            result = partyId;
            messageLog.getMessageId();
            result = messageId;
            messageLog.getMpc();
            result = mpc;
            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pmodeKey;
            pModeProvider.getLegConfiguration(pmodeKey);
            result = legConfiguration;
            pullMessageService.getPullMessageExpirationDate(messageLog, legConfiguration);
            result = staledDate;
        }};
        pullMessageService.addPullMessageLock(partyIdExtractor, userMessage, messageLog);
        new Verifications() {{
            MessagingLock messagingLock = null;
            messagingLockDao.releaseLock(messagingLock = withCapture());
            assertEquals(partyId, messagingLock.getInitiator());
            assertEquals(mpc, messagingLock.getMpc());
            assertEquals(messageId, messagingLock.getMessageId());
            assertEquals(staledDate, messagingLock.getStaled());

        }};
    }


    @Test
    public void getPullMessageExpirationDate(@Mocked final MessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration) {

        final int extraAttemptsToAddToExpirationDate = 2;
        new MockUp<PullMessageServiceImpl>() {
            @Mock
            public int getExtraNumberOfAttemptTimeForExpirationDate() {
                return extraAttemptsToAddToExpirationDate;
            }
        };

        final long currentTime = System.currentTimeMillis();
        final int timeOut = 10;
        final long timeOutInMillis = 60000 * timeOut;
        final int retryCount = 3;
        final Date expectedDate = new Date(currentTime + timeOutInMillis + (extraAttemptsToAddToExpirationDate * (timeOutInMillis / retryCount)));
        new Expectations() {{
            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = currentTime;
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = timeOut;
            legConfiguration.getReceptionAwareness().getRetryCount();
            result = retryCount;
        }};
        assertEquals(expectedDate, pullMessageService.getPullMessageExpirationDate(userMessageLog, legConfiguration));

    }

    @Test
    public void getExtraNumberOfAttemptTimeForExpirationDate() {
        new Expectations() {{
            domibusProperties.getProperty("pull.extra.number.of.attempt.time.for.expiration.date", "2");
            result = "2";
        }};
        assertEquals(2, pullMessageService.getExtraNumberOfAttemptTimeForExpirationDate());
    }

    @Test
    public void waitingForCallBackWithoutAttempt(@Mocked final ToExtractor toExtractor, @Mocked final UserMessage userMessage, @Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {
        final String messageID = "123456";
        new MockUp<PullMessageServiceImpl>() {
            @Mock
            public boolean hasAttemptsLeft(final MessageLog userMessageLog, final LegConfiguration legConfiguration) {
                return false;
            }

            @Mock
            public void addPullMessageLock(final PartyIdExtractor partyIdExtractor, UserMessage userMessage, MessageLog messageLog) {
            }
        };
        pullMessageService.waitingForCallBack(userMessage, legConfiguration, userMessageLog);
        new Verifications() {{
            updateRetryLoggingService.updateMessageLogNextAttemptDate(legConfiguration, userMessageLog);
            times = 0;
            userMessageLogDao.update(userMessageLog);
            userMessage.getPartyInfo().getTo();
            times = 1;
            backendNotificationService.notifyOfMessageStatusChange(userMessageLog, MessageStatus.WAITING_FOR_RECEIPT, withAny(new Timestamp(System.currentTimeMillis())));
            pullMessageService.addPullMessageLock(toExtractor, userMessage, userMessageLog);
            times = 1;
        }};
    }

    @Test
    public void waitingForCallBackWithAttempt(@Mocked final ToExtractor toExtractor, @Mocked final UserMessage userMessage, @Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {
        final String messageID = "123456";
        new MockUp<PullMessageServiceImpl>() {
            @Mock
            public boolean hasAttemptsLeft(final MessageLog userMessageLog, final LegConfiguration legConfiguration) {
                return true;
            }

            @Mock
            public void addPullMessageLock(final PartyIdExtractor partyIdExtractor, UserMessage userMessage, MessageLog messageLog) {
            }
        };
        pullMessageService.waitingForCallBack(userMessage, legConfiguration, userMessageLog);
        new Verifications() {{
            updateRetryLoggingService.updateMessageLogNextAttemptDate(legConfiguration, userMessageLog);
            times = 1;
            userMessageLogDao.update(userMessageLog);
            userMessage.getPartyInfo().getTo();
            times = 1;
            backendNotificationService.notifyOfMessageStatusChange(userMessageLog, MessageStatus.WAITING_FOR_RECEIPT, withAny(new Timestamp(System.currentTimeMillis())));
            pullMessageService.addPullMessageLock(toExtractor, userMessage, userMessageLog);
        }};
    }

    @Test
    public void hasAttemptsLeftTrueBecauseOfSendAttempt(@Mocked final MessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = 1;
            userMessageLog.getSendAttempts();
            result = 1;
            userMessageLog.getSendAttemptsMax();
            result = 2;
            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = System.currentTimeMillis() - 50000;
        }};
        assertEquals(true, pullMessageService.attemptNumberLeftIsLowerOrEqualThenMaxAttempts(userMessageLog, legConfiguration));
    }

    @Test
    public void hasAttemptsLeftFalseBecauseOfSendAttempt(@Mocked final MessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            times = 0;
            userMessageLog.getSendAttempts();
            result = 3;
            userMessageLog.getSendAttemptsMax();
            result = 2;
            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            times = 0;
        }};
        assertEquals(false, pullMessageService.attemptNumberLeftIsLowerOrEqualThenMaxAttempts(userMessageLog, legConfiguration));
    }

    @Test
    public void hasAttemptsLeftFalseBecauseOfRetry(@Mocked final MessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = 1;
            userMessageLog.getSendAttempts();
            result = 2;
            userMessageLog.getSendAttemptsMax();
            result = 2;
            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = System.currentTimeMillis() - 70000;
        }};
        assertEquals(false, pullMessageService.attemptNumberLeftIsLowerOrEqualThenMaxAttempts(userMessageLog, legConfiguration));
    }

    @Test
    public void pullFailedOnRequestWithNoAttempt(@Mocked final ToExtractor toExtractor, @Mocked final UserMessage userMessage, @Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {

        final String messageID = "123456";
        new MockUp<PullMessageServiceImpl>() {
            @Mock
            public void addPullMessageLock(final PartyIdExtractor partyIdExtractor, UserMessage userMessage, MessageLog messageLog) {
            }
        };
        new Expectations() {{
            userMessageLog.getMessageId();
            result = messageID;
            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = false;
        }};

        pullMessageService.pullFailedOnRequest(userMessage, legConfiguration, userMessageLog);
        new VerificationsInOrder() {{
            pullMessageStateService.sendFailed(userMessageLog);
        }};
    }

    @Test
    public void pullFailedOnRequestWithAttempt(@Mocked final ToExtractor toExtractor, @Mocked final UserMessage userMessage, @Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {

        final String messageID = "123456";
        new MockUp<PullMessageServiceImpl>() {
            @Mock
            public void addPullMessageLock(final PartyIdExtractor partyIdExtractor, UserMessage userMessage, MessageLog messageLog) {
            }
        };
        new Expectations() {{
            userMessageLog.getMessageId();
            result = messageID;
            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = true;
        }};

        pullMessageService.pullFailedOnRequest(userMessage, legConfiguration, userMessageLog);
        new VerificationsInOrder() {{
            updateRetryLoggingService.increaseAttempAndNotify(legConfiguration, MessageStatus.READY_TO_PULL, userMessageLog);
            times = 1;
            pullMessageService.addPullMessageLock(toExtractor, userMessage, userMessageLog);
            times = 1;
        }};
    }

    @Test
    public void pullFailedOnReceiptWithAttemptLeft(@Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {
        final String messageID = "123456";
        new Expectations() {{
            userMessageLog.getMessageId();
            result = messageID;
            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = true;
        }};
        pullMessageService.pullFailedOnReceipt(legConfiguration, userMessageLog, null);
        new VerificationsInOrder() {{
            rawEnvelopeLogDao.deleteUserMessageRawEnvelope(messageID);
            times = 1;
            backendNotificationService.notifyOfMessageStatusChange(userMessageLog, MessageStatus.READY_TO_PULL, withAny(new Timestamp(0)));
            times = 1;
            pullMessageStateService.reset(userMessageLog);
            times = 1;
        }};

    }

    @Test
    public void pullFailedOnReceiptWithNoAttemptLeft(@Mocked final LegConfiguration legConfiguration, @Mocked final UserMessageLog userMessageLog) {
        final String messageID = "123456";
        new Expectations() {{
            userMessageLog.getMessageId();
            result = messageID;
            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = false;
        }};
        pullMessageService.pullFailedOnReceipt(legConfiguration, userMessageLog, null);
        new VerificationsInOrder() {{
            messagingLockDao.delete(messageID);
            times = 1;
            pullMessageStateService.sendFailed(userMessageLog);
            times = 1;
        }};

    }
}