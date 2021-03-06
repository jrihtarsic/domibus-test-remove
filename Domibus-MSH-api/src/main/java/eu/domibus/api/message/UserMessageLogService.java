package eu.domibus.api.message;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
public interface UserMessageLogService {

    /**
     * Saves a UserMessageLog entry for the given parameters
     */
    void save(String messageId, String messageStatus, String notificationStatus, String mshRole, Integer maxAttempts, String mpc, String backendName, String endpoint, String service, String action, Boolean sourceMessage, Boolean messageFragment);

    void setMessageAsDeleted(String messageId);

}
