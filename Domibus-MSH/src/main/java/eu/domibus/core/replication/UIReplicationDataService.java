package eu.domibus.core.replication;

import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;

/**
 * Replication Data service interface - all operations of writing data into {@code TB_MESSAGE_UI} table
 *
 * @author Catalin Enache
 * @since 4.0
 */
public interface UIReplicationDataService {

    /**
     * replicates data on receiver side when a new user message is received
     *
     * @param messageId
     * @param jmsTimestamp
     */
    void messageReceived(final String messageId, long jmsTimestamp);

    /**
     * replicates data on sender side when a new user message is submitted
     *
     * @param messageId
     * @param jmsTimestamp
     */
    void messageSubmitted(final String messageId, final long jmsTimestamp);

    /**
     * updates/sync data on receiver/sender side when a change in messages status appears
     *
     *  @param messageId
     * @param messageStatus
     * @param jmsTimestamp
     */
    void messageStatusChange(final String messageId, MessageStatus messageStatus, long jmsTimestamp);

    /**
     * updates/sync data on receiver/sender side when a change in messages notification status appears
     *
     * @param messageId
     * @param notificationStatus
     * @param jmsTimestamp
     */
    void messageNotificationStatusChange(final String messageId, NotificationStatus notificationStatus, long jmsTimestamp);

    /**
     * updates/sync data on receiver/sender side when a change in message change appears
     *
     * @param messageId
     * @param jmsTimestamp
     */
    void messageChange(final String messageId, final long jmsTimestamp);

    /**
     * replicates data on sender side when a new signal message is submitted
     *
     * @param messageId
     * @param jmsTimestamp
     */
    void signalMessageSubmitted(final String messageId, final long jmsTimestamp);

    /**
     * replicates data on receiver side when a new signal message is received
     *
     * @param messageId
     * @param jmsTimestamp
     */
    void signalMessageReceived(final String messageId, final long jmsTimestamp);

    /**
     * run the diff query against {@code V_MESSAGE_UI} view and sync the data
     */
    void findAndSyncUIMessages();

    /**
     * run the diff query against {@code V_MESSAGE_UI} view and sync the data
     */
    int findAndSyncUIMessages(int limit);

    /**
     * count no of records from  {@code V_MESSAGE_UI}
     *
     * @return no of records
     */
    int countSyncUIMessages();

}
