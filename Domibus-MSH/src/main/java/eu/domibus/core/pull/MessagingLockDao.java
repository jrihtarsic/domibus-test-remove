package eu.domibus.core.pull;

import eu.domibus.ebms3.common.model.MessagingLock;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Thomas Dussart
 * @since 3.3.4
 */
public interface MessagingLockDao {


    PullMessageId getOracleNextPullMessageToProcess(final String initiator, final String mpc);

    PullMessageId getMessageIdInTransaction(Long idPk);

    PullMessageId getNextPullMessageToProcess(Integer messageId);

    MessagingLock getLock(String messageId);

    void save(MessagingLock messagingLock);

    void delete(String messageId);

    void delete(MessagingLock messagingLock);

    MessagingLock findMessagingLockForMessageId(String messageId);

    List<MessagingLock> findStaledMessages();

    List<MessagingLock> findDeletedMessages();

    List<MessagingLock> findReadyToPull(String mpc, String initiator);

    List<MessagingLock> findWaitingForReceipt();

}
