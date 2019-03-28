package eu.domibus.core.pull;

import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.core.mpc.MpcService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.ebms3.common.model.MessageState;
import eu.domibus.ebms3.common.model.MessagingLock;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.ebms3.sender.UpdateRetryLoggingService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)

public class PullMessageServiceTest {

    private final static String MESSAGE_ID = "MESSAGE_ID";

    private final static String MPC = "MPC";

    @Tested
    private PullMessageServiceImpl pullService;

    @Injectable
    private MessagingLockDao messagingLockDao;

    @Injectable
    MpcService mpcService;

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
    private PModeProvider pModeProvider;

    @Injectable
    private java.util.Properties domibusProperties;

    @Injectable
    private UIReplicationSignalService uiReplicationSignalService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    @Ignore
    public void pullMessageForTheFirstTime() {
        final String initiator = "initiator";


        new Expectations() {
            {
                messagingLockDao.getNextPullMessageToProcess(6);
                result = MESSAGE_ID;
            }
        };
        String pullMessageId = pullService.getPullMessageId(initiator, MPC);
        assertEquals(MESSAGE_ID, pullMessageId);
    }

    @Test
    @Ignore
    public void addSearchInFormation(@Mocked final PartyIdExtractor partyIdExtractor) {

        final String partyId = "partyId";
        new Expectations() {
            {
                partyIdExtractor.getPartyId();
                result = partyId;
            }
        };
        pullService.addPullMessageLock(partyIdExtractor, "", null);
        new Verifications() {{
            MessagingLock messagingLock = new MessagingLock();
            // messagingLockDao.releaseLock(messagingLock = withCapture());
            //assertEquals(MESSAGE_ID, messagingLock.getMessageId());
            assertEquals(MessageState.READY, messagingLock.getMessageState());
            assertEquals(MessagingLock.PULL, messagingLock.getMessageType());
            assertEquals(MPC, messagingLock.getMpc());
        }};
    }

    @Test
    public void delete() {
        pullService.deletePullMessageLock(MESSAGE_ID);
        new Verifications() {{
            messagingLockDao.delete(MESSAGE_ID);
        }};
    }
}