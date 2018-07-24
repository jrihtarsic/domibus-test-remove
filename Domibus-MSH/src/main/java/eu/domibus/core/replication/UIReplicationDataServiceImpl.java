package eu.domibus.core.replication;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.SignalMessageLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.SignalMessageLog;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.util.WarningUtil;
import eu.domibus.ebms3.common.UserMessageDefaultServiceHelper;
import eu.domibus.ebms3.common.model.MessageType;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Service dedicate to replicate
 * data in <code>TB_MESSAGE_UI</> table
 * It first reads existing data and then insert it
 *
 * @author Cosmin Baciu, Catalin Enache
 * @since 4.0
 */
@Service
public class UIReplicationDataServiceImpl implements UIReplicationDataService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UIReplicationDataServiceImpl.class);

    private static final String MAX_ROWS_KEY = "domibus.ui.replication.sync.cron.max.rows";

    private int maxRowsToSync;

    @Autowired
    private UIMessageDaoImpl uiMessageDao;

    @Autowired
    private UIMessageDiffDao uiMessageDiffDao;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private SignalMessageLogDao signalMessageLogDao;

    @Autowired
    private UserMessageDefaultServiceHelper userMessageDefaultServiceHelper;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @PostConstruct
    public void init() {
        maxRowsToSync = NumberUtils.toInt(domibusPropertyProvider.getDomainProperty(MAX_ROWS_KEY, "1000"));
    }

    @Override
    public void messageReceived(String messageId) {
        saveUIMessageFromUserMessageLog(messageId);
    }

    @Override
    public void messageSubmitted(String messageId) {
        saveUIMessageFromUserMessageLog(messageId);
    }

    @Override
    public void messageStatusChange(String messageId) {
        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        final UIMessageEntity entity = uiMessageDao.findUIMessageByMessageId(messageId);

        if (entity != null) {
            entity.setMessageStatus(userMessageLog.getMessageStatus());
            entity.setDeleted(userMessageLog.getDeleted());
            entity.setNextAttempt(userMessageLog.getNextAttempt());
            entity.setFailed(userMessageLog.getFailed());

            try {
                uiMessageDao.update(entity);
                uiMessageDao.flush();
            } catch (StaleObjectStateException | OptimisticLockException e) {
                LOG.debug("Optimistic lock detected for messageStatusChange on messageId={}", messageId);
            }
        } else {
            UIReplicationDataServiceImpl.LOG.warn("messageStatusChange failed for messageId={}", messageId);
        }
        LOG.debug("{}Message with messageId={} synced, status={}",
                MessageType.USER_MESSAGE.equals(userMessageLog.getMessageType()) ? "User" : "Signal", messageId,
                userMessageLog.getMessageStatus());
    }

    @Override
    public void messageNotificationStatusChange(String messageId) {
        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        final UIMessageEntity entity = uiMessageDao.findUIMessageByMessageId(messageId);

        if (entity != null) {
            entity.setNotificationStatus(userMessageLog.getNotificationStatus());

            try {
                uiMessageDao.update(entity);
                uiMessageDao.flush();
            } catch (StaleObjectStateException | OptimisticLockException e) {
                LOG.debug("Optimistic lock detected for messageNotificationStatusChange on messageId={}", messageId);
            }
        } else {
            UIReplicationDataServiceImpl.LOG.warn("messageNotificationStatusChange failed for messageId={}", messageId);
        }
        LOG.debug("{}Message with messageId={} synced, notificationStatus={}",
                MessageType.USER_MESSAGE.equals(userMessageLog.getMessageType()) ? "User" : "Signal", messageId,
                userMessageLog.getNotificationStatus());

    }

    @Override
    public void messageChange(String messageId) {

        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        final UIMessageEntity entity = uiMessageDao.findUIMessageByMessageId(messageId);

        if (entity != null) {
            updateUIMessage(userMessageLog, entity);

            uiMessageDao.update(entity);
        } else {
            UIReplicationDataServiceImpl.LOG.warn("messageChange failed for messageId={}", messageId);
        }
        LOG.debug("{}Message with messageId={} synced",
                MessageType.USER_MESSAGE.equals(userMessageLog.getMessageType()) ? "User" : "Signal", messageId);
    }

    @Override
    public void signalMessageSubmitted(final String messageId) {
        saveUIMessageFromSignalMessageLog(messageId);
    }

    /**
     * {@inheritDoc}
     *
     * @param messageId
     */
    @Override
    public void signalMessageReceived(String messageId) {
        saveUIMessageFromSignalMessageLog(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void findAndSyncUIMessages() {
        LOG.debug("start counting differences for UIReplication");

        int rowsToSyncCount = uiMessageDiffDao.countAll();
        LOG.info("Found {} differences between native tables and TB_MESSAGE_UI", rowsToSyncCount);

        if (rowsToSyncCount > maxRowsToSync) {
            LOG.warn(WarningUtil.warnOutput("There are more than {} rows to sync into TB_MESSAGE_UI table " +
                    "please use the REST resource instead."), maxRowsToSync);
            return;
        }

        List<UIMessageEntity> uiMessageEntityList =
                uiMessageDiffDao.findAll().
                        stream().
                        map(objects -> convertToUIMessageEntity(objects)).
                        collect(Collectors.toList());

        if (uiMessageEntityList.size() > 0) {
            LOG.info("start to update TB_MESSAGE_UI");
            try {
                uiMessageEntityList.parallelStream().forEach(uiMessageEntity -> uiMessageDao.saveOrUpdate(uiMessageEntity));
            } catch (OptimisticLockException e) {
                LOG.warn("Optimistic lock exception detected");
            }
            LOG.info("finish to update TB_MESSAGE_UI");
        }
    }

    /**
     * Replicates {@link SignalMessage} into {@code TB_MESSAGE_UI} table as {@link UIMessageEntity}
     *
     * @param messageId
     */
    private void saveUIMessageFromSignalMessageLog(String messageId) {
        final SignalMessageLog signalMessageLog = signalMessageLogDao.findByMessageId(messageId);
        final SignalMessage signalMessage = messagingDao.findSignalMessageByMessageId(messageId);

        final Messaging messaging = messagingDao.findMessageByMessageId(signalMessage.getMessageInfo().getRefToMessageId());
        final UserMessage userMessage = messaging.getUserMessage();

        UIMessageEntity entity = new UIMessageEntity();
        entity.setMessageId(messageId);
        entity.setMessageStatus(signalMessageLog.getMessageStatus());
        entity.setNotificationStatus(signalMessageLog.getNotificationStatus());
        entity.setMshRole(signalMessageLog.getMshRole());
        entity.setMessageType(signalMessageLog.getMessageType());

        entity.setDeleted(signalMessageLog.getDeleted());
        entity.setReceived(signalMessageLog.getReceived());
        entity.setSendAttempts(signalMessageLog.getSendAttempts());
        entity.setSendAttemptsMax(signalMessageLog.getSendAttemptsMax());
        entity.setNextAttempt(signalMessageLog.getNextAttempt());
        entity.setConversationId(StringUtils.EMPTY);
        entity.setFromId(userMessage.getPartyInfo().getFrom().getPartyId().iterator().next().getValue());
        entity.setToId(userMessage.getPartyInfo().getTo().getPartyId().iterator().next().getValue());
        entity.setFromScheme(userMessageDefaultServiceHelper.getOriginalSender(userMessage));
        entity.setToScheme(userMessageDefaultServiceHelper.getFinalRecipient(userMessage));
        entity.setRefToMessageId(signalMessage.getMessageInfo().getRefToMessageId());
        entity.setFailed(signalMessageLog.getFailed());
        entity.setRestored(signalMessageLog.getRestored());
        entity.setMessageSubtype(signalMessageLog.getMessageSubtype());

        uiMessageDao.create(entity);
        LOG.debug("SignalMessage with messageId={} replicated", messageId);
    }

    /**
     * Replicates {@link UserMessage} into {@code TB_MESSAGE_UI} table as {@link UIMessageEntity}
     *
     * @param messageId
     */
    private void saveUIMessageFromUserMessageLog(String messageId) {
        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        final UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);

        UIMessageEntity entity = new UIMessageEntity();
        entity.setMessageId(messageId);
        entity.setMessageStatus(userMessageLog.getMessageStatus());
        entity.setNotificationStatus(userMessageLog.getNotificationStatus());
        entity.setMshRole(userMessageLog.getMshRole());
        entity.setMessageType(userMessageLog.getMessageType());
        entity.setDeleted(userMessageLog.getDeleted());
        entity.setReceived(userMessageLog.getReceived());
        entity.setSendAttempts(userMessageLog.getSendAttempts());
        entity.setSendAttemptsMax(userMessageLog.getSendAttemptsMax());
        entity.setNextAttempt(userMessageLog.getNextAttempt());
        entity.setConversationId(userMessage.getCollaborationInfo().getConversationId());
        entity.setFromId(userMessage.getPartyInfo().getFrom().getPartyId().iterator().next().getValue());
        entity.setToId(userMessage.getPartyInfo().getTo().getPartyId().iterator().next().getValue());
        entity.setFromScheme(userMessageDefaultServiceHelper.getOriginalSender(userMessage));
        entity.setToScheme(userMessageDefaultServiceHelper.getFinalRecipient(userMessage));
        entity.setRefToMessageId(userMessage.getMessageInfo().getRefToMessageId());
        entity.setFailed(userMessageLog.getFailed());
        entity.setRestored(userMessageLog.getRestored());
        entity.setMessageSubtype(userMessageLog.getMessageSubtype());

        uiMessageDao.create(entity);
        LOG.debug("UserMessage with messageId={} replicated", messageId);
    }

    /**
     * Updates {@link UIMessageEntity} fields with info from {@link UserMessageLog}
     *
     * @param userMessageLog
     * @param entity
     */
    private void updateUIMessage(UserMessageLog userMessageLog, UIMessageEntity entity) {
        entity.setMessageStatus(userMessageLog.getMessageStatus());
        entity.setNotificationStatus(userMessageLog.getNotificationStatus());
        entity.setDeleted(userMessageLog.getDeleted());
        entity.setFailed(userMessageLog.getFailed());
        entity.setRestored(userMessageLog.getRestored());
        entity.setNextAttempt(userMessageLog.getNextAttempt());
        entity.setSendAttempts(userMessageLog.getSendAttempts());
        entity.setSendAttemptsMax(userMessageLog.getSendAttemptsMax());
    }

    /**
     * Converts one record of the diff query to {@link UIMessageEntity}
     *
     * @param diffEntity
     * @return
     */
    private UIMessageEntity convertToUIMessageEntity(UIMessageDiffEntity diffEntity) {
        if (null == diffEntity) {
            return null;
        }

        UIMessageEntity entity = new UIMessageEntity();
        entity.setMessageId(diffEntity.getMessageId());
        entity.setMessageStatus(diffEntity.getMessageStatus());
        entity.setNotificationStatus(diffEntity.getNotificationStatus());
        entity.setMshRole(diffEntity.getMshRole());
        entity.setMessageType(diffEntity.getMessageType());
        entity.setDeleted(diffEntity.getDeleted());
        entity.setReceived(diffEntity.getReceived());
        entity.setSendAttempts(diffEntity.getSendAttempts());
        entity.setSendAttemptsMax(diffEntity.getSendAttemptsMax());
        entity.setNextAttempt(diffEntity.getNextAttempt());
        entity.setConversationId(diffEntity.getConversationId());
        entity.setFromId(diffEntity.getFromId());
        entity.setToId(diffEntity.getToId());
        entity.setFromScheme(diffEntity.getFromScheme());
        entity.setToScheme(diffEntity.getToScheme());
        entity.setRefToMessageId(diffEntity.getRefToMessageId());
        entity.setFailed(diffEntity.getFailed());
        entity.setRestored(diffEntity.getRestored());
        entity.setMessageSubtype(diffEntity.getMessageSubtype());

        return entity;
    }


}
