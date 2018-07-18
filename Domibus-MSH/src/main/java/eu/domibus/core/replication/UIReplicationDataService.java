package eu.domibus.core.replication;

import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.model.logging.UserMessageLog;

import java.util.List;

/**
 * @author Catalin Enache
 * @since 4.0
 *
 */
public interface UIReplicationDataService {

    /**
     * replicates data on receiver side when a new messages is received
     *
     * @param messageId
     */
    void messageReceived(final String messageId);

    /**
     * replicates data on sender side when a new message is submitted
     * @param messageId
     */
    void messageSubmitted(final String messageId);

    /**
     * updates/sync data on receiver/sender side when a change in messages status appears
     *
     * @param messageId
     * @param newStatus
     */
    void messageStatusChange(final String messageId, MessageStatus newStatus);

    /**
     * updates/sync data on receiver/sender side when a change in messages notification status appears
     * @param messageId
     * @param newStatus
     */
    void messageNotificationStatusChange(final String messageId, NotificationStatus newStatus);

    void messageChange(final String messageId);

    void signalMessageSubmitted(final String messageId);

    void signalMessageReceived(final String messageId);

    public void findAndSyncUIMessages();

}
