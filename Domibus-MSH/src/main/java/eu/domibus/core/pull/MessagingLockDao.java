package eu.domibus.core.pull;

import eu.domibus.ebms3.common.model.MessagingLock;

import java.util.List;

/**
 * @author Thomas Dussart
 * @since 3.3.4
 */
public interface MessagingLockDao {

    String getNextPullMessageToProcess(String messageType, String initiator, String mpc);

    void save(MessagingLock messagingLock);

    void delete(String messageId);

}
