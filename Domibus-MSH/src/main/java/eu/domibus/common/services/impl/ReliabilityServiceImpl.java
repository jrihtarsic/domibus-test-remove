package eu.domibus.common.services.impl;

import eu.domibus.api.message.UserMessageLogService;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.common.services.ReliabilityService;
import eu.domibus.ebms3.common.model.PartyInfo;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.ebms3.sender.ReliabilityChecker;
import eu.domibus.ebms3.sender.ResponseHandler;
import eu.domibus.ebms3.sender.UpdateRetryLoggingService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Dussart
 * @author Cosmin Baciu
 * @since 3.3
 */

@Service
public class ReliabilityServiceImpl implements ReliabilityService {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReliabilityServiceImpl.class);

    @Autowired
    private UserMessageLogService userMessageLogService;

    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Autowired
    private UserMessageHandlerService userMessageHandlerService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleReliability(final String messageId, UserMessage userMessage, final ReliabilityChecker.CheckResult reliabilityCheckSuccessful, final ResponseHandler.CheckResult isOk, final LegConfiguration legConfiguration) {
        changeMessageStatusAndNotify(messageId, userMessage, reliabilityCheckSuccessful, isOk, legConfiguration);
    }

    private void changeMessageStatusAndNotify(String messageId, UserMessage userMessage, ReliabilityChecker.CheckResult reliabilityCheckSuccessful, ResponseHandler.CheckResult isOk, LegConfiguration legConfiguration) {
        final Boolean isTestMessage = userMessageHandlerService.checkTestMessage(legConfiguration);
        final MessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING);

        switch (reliabilityCheckSuccessful) {
            case OK:
                switch (isOk) {
                    case OK:
                        userMessageLogService.setMessageAsAcknowledged(messageId);
                        break;
                    case WARNING:
                        userMessageLogService.setMessageAsAckWithWarnings(messageId);
                        break;
                    default:
                        assert false;
                }
                if (!isTestMessage) {
                    backendNotificationService.notifyOfSendSuccess(messageId);
                }
                userMessageLog.setSendAttempts(userMessageLog.getSendAttempts() + 1);
                messagingDao.clearPayloadData(messageId);
                final PartyInfo partyInfo = userMessage.getPartyInfo();
                LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_SEND_SUCCESS, messageId, partyInfo.getFrom().getFirstPartyId(), partyInfo.getTo().getFirstPartyId());
                break;
            case WAITING_FOR_CALLBACK:
                updateRetryLoggingService.updateWaitingReceiptMessageRetryLogging(messageId, legConfiguration);
                break;
            case SEND_FAIL:
                updateRetryLoggingService.updatePushedMessageRetryLogging(messageId, legConfiguration);
                break;
            case ABORT:
                updateRetryLoggingService.messageFailedInANewTransaction(userMessage, userMessageLog);
                break;
        }
    }
}
