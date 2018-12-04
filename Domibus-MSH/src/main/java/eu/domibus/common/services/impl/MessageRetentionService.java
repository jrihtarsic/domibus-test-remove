package eu.domibus.common.services.impl;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * This service class is responsible for the retention and clean up of Domibus messages, including signal messages.
 * Notice that only payloads data are really deleted.
 *
 * @author Christian Koch, Stefan Mueller, Federico Martini, Cosmin Baciu
 * @since 3.0
 */
@Service
public class MessageRetentionService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageRetentionService.class);

    public static final String DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_DOWNLOADED_MAX_DELETE = "domibus.retentionWorker.message.retention.downloaded.max.delete";
    public static final String DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_NOT_DOWNLOADED_MAX_DELETE = "domibus.retentionWorker.message.retention.not_downloaded.max.delete";
    public static final String DOMIBUS_ATTACHMENT_STORAGE_LOCATION = "domibus.attachment.storage.location";

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private UserMessageService userMessageService;


    /**
     * Deletes the expired messages(downloaded or not) using the configured limits
     */
    @Transactional
    public void deleteExpiredMessages() {
        final List<String> mpcs = pModeProvider.getMpcURIList();
        final Integer expiredDownloadedMessagesLimit = getRetentionValue(DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_DOWNLOADED_MAX_DELETE);
        final Integer expiredNotDownloadedMessagesLimit = getRetentionValue(DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_NOT_DOWNLOADED_MAX_DELETE);
        for (final String mpc : mpcs) {
            deleteExpiredMessages(mpc, expiredDownloadedMessagesLimit, expiredNotDownloadedMessagesLimit);
        }
    }

    /**
     * Deletes all expired messages
     */
    @Transactional
    public void deleteAllExpiredMessages() {
        final List<String> mpcs = pModeProvider.getMpcURIList();
        final Integer expiredDownloadedMessagesLimit = Integer.MAX_VALUE;
        final Integer expiredNotDownloadedMessagesLimit = Integer.MAX_VALUE;

        for (final String mpc : mpcs) {
            deleteExpiredMessages(mpc, expiredDownloadedMessagesLimit, expiredNotDownloadedMessagesLimit);
        }
    }

    @Transactional
    public void deleteExpiredMessages(String mpc, Integer expiredDownloadedMessagesLimit, Integer expiredNotDownloadedMessagesLimit) {
        LOG.debug("Deleting expired messages for MPC [{}] using expiredDownloadedMessagesLimit [{}]" +
                " and expiredNotDownloadedMessagesLimit [{}]", mpc, expiredDownloadedMessagesLimit, expiredNotDownloadedMessagesLimit);
        deleteExpiredDownloadedMessages(mpc, expiredDownloadedMessagesLimit);
        deleteExpiredNotDownloadedMessages(mpc, expiredNotDownloadedMessagesLimit);
    }

    protected void deleteExpiredDownloadedMessages(String mpc, Integer expiredDownloadedMessagesLimit) {
        LOG.debug("Deleting expired downloaded messages for MPC [{}] using expiredDownloadedMessagesLimit [{}]", mpc, expiredDownloadedMessagesLimit);
        final int messageRetentionDownloaded = pModeProvider.getRetentionDownloadedByMpcURI(mpc);
        String fileLocation = domibusPropertyProvider.getDomainProperty(DOMIBUS_ATTACHMENT_STORAGE_LOCATION);
        // If messageRetentionDownloaded is equal to -1, the messages will be kept indefinitely and, if 0 and no file system storage was used, they have already been deleted during download operation.
        if (messageRetentionDownloaded > 0 || (StringUtils.isNotEmpty(fileLocation) && messageRetentionDownloaded >= 0)) {
            List<String> downloadedMessageIds = userMessageLogDao.getDownloadedUserMessagesOlderThan(DateUtils.addMinutes(new Date(), messageRetentionDownloaded * -1),
                    mpc, expiredDownloadedMessagesLimit);
            if (CollectionUtils.isNotEmpty(downloadedMessageIds)) {
                final int deleted = downloadedMessageIds.size();
                LOG.debug("Found [{}] downloaded messages to delete", deleted);
                userMessageService.delete(downloadedMessageIds);
                LOG.debug("Deleted [{}] downloaded messages", deleted);
            }
        }
    }

    protected void deleteExpiredNotDownloadedMessages(String mpc, Integer expiredNotDownloadedMessagesLimit) {
        LOG.debug("Deleting expired not-downloaded messages for MPC [{}] using expiredNotDownloadedMessagesLimit [{}]", mpc, expiredNotDownloadedMessagesLimit);
        final int messageRetentionNotDownloaded = pModeProvider.getRetentionUndownloadedByMpcURI(mpc);
        if (messageRetentionNotDownloaded > -1) { // if -1 the messages will be kept indefinitely and if 0, although it makes no sense, is legal
            final List<String> notDownloadedMessageIds = userMessageLogDao.getUndownloadedUserMessagesOlderThan(DateUtils.addMinutes(new Date(), messageRetentionNotDownloaded * -1),
                    mpc, expiredNotDownloadedMessagesLimit);
            if (CollectionUtils.isNotEmpty(notDownloadedMessageIds)) {
                final int deleted = notDownloadedMessageIds.size();
                LOG.debug("Found [{}] not-downloaded messages to delete", deleted);
                userMessageService.delete(notDownloadedMessageIds);
                LOG.debug("Deleted [{}] not-downloaded messages", deleted);
            }
        }
    }

    protected Integer getRetentionValue(String propertyName) {
        final String propertyValueString = domibusPropertyProvider.getDomainProperty(propertyName);
        return Integer.parseInt(propertyValueString);
    }

}
