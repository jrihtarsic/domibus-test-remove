package eu.domibus.core.pull;

import eu.domibus.common.model.logging.UserMessageLogEntity;

/**
 * @author Thomas Dussart
 * @since 4.0
 *
 * In the case of a pull mechanism when a message is retrieved, there is a need for a lock mechanism to occur in order
 * to avoid that a message is pulled twice. This service is in charge of this behavior.
 */
public interface PullMessageStateService {


    /**
     * Handle the state management of a staled pull message.²
     * @param messageId the message id.
     */
    void expirePullMessage(String messageId);

    /**
     * Reset the next attempt date, put the message in send_failure and notify if configure.
     *
     * @param userMessageLog the user message.
     */
    void sendFailed(UserMessageLogEntity userMessageLog);


    /**
     * Reset a failed message into ready to pull..
     *
     * @param userMessageLog the user message.
     */
    void reset(UserMessageLogEntity userMessageLog);

}
