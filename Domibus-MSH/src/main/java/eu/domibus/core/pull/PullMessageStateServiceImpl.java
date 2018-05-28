package eu.domibus.core.pull;

import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.RawEnvelopeLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.ebms3.sender.UpdateRetryLoggingService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Dussart
 * @since 3.3.3
 * <p>
 * {@inheritDoc}
 */
@Service
public class PullMessageStateServiceImpl implements PullMessageStateService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PullMessageStateServiceImpl.class);

    @Autowired
    private RawEnvelopeLogDao rawEnvelopeLogDao;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Autowired
    private MessagingLockDao messagingLockDao;

    @Autowired
    private MessagingDao messagingDao;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirePullMessage(final String messageId) {
        LOG.debug("Message:[{}] expired.", messageId);
        messagingLockDao.deleteLock(messageId);
        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        rawEnvelopeLogDao.deleteUserMessageRawEnvelope(messageId);
        sendFailed(userMessageLog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void sendFailed(UserMessageLog userMessageLog) {
        LOG.debug("Message:[{}] failed to be pull.", userMessageLog.getMessageId());
        // userMessageLog.setNextAttempt(null);
        updateRetryLoggingService.messageFailed(userMessageLog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(UserMessageLog userMessageLog) {
        final MessageStatus readyToPull = MessageStatus.READY_TO_PULL;
        LOG.debug("Change message:[{}] with state:[{}] to state:[{}].", userMessageLog.getMessageId(), userMessageLog.getMessageStatus(), readyToPull);
        userMessageLog.setMessageStatus(readyToPull);
        userMessageLogDao.update(userMessageLog);
    }


}
