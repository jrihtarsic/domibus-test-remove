package eu.domibus.core.message;

import eu.domibus.ebms3.common.model.UserMessage;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
public interface MessageServiceHelper {

    String getOriginalSender(UserMessage userMessage);

    String getFinalRecipient(UserMessage userMessage);

    boolean isSameOriginalSender(UserMessage userMessage, String originalSender);

    boolean isSameFinalRecipient(UserMessage userMessage, String originalSender);
}
