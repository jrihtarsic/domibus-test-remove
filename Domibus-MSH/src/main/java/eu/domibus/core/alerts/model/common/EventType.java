package eu.domibus.core.alerts.model.common;

import eu.domibus.logging.DomibusMessageCode;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public enum EventType {
    MSG_STATUS_CHANGED,
    CERT_IMMINENT_EXPIRATION,
    CERT_EXPIRED,
    USER_LOGIN_FAILURE,
    USER_ACCOUNT_DISABLED,

    PASSWORD_EXPIRED,
    PASSWORD_IMMINENT_EXPIRATION;


    public static String getQueueSelectorFromEventType(EventType eventType) {
        switch (eventType) {
            case MSG_STATUS_CHANGED:
                return "message";
            case CERT_IMMINENT_EXPIRATION:
                return "certificateImminentExpiration";
            case CERT_EXPIRED:
                return "certificateExpired";
            case USER_LOGIN_FAILURE:
                return "loginFailure";
            case USER_ACCOUNT_DISABLED:
                return "accountDisabled";
            case PASSWORD_IMMINENT_EXPIRATION:
                return "userPasswordImminentExpiration";
            case PASSWORD_EXPIRED:
                return "userPasswordExpired";

            default:
                throw new IllegalStateException("Selector for event type " + eventType + " not defined");
        }
    }

    public static DomibusMessageCode getSecurityMessageCode(EventType eventType) {
        switch (eventType) {

            case PASSWORD_IMMINENT_EXPIRATION:
                return DomibusMessageCode.SEC_PASSWORD_IMMINENT_EXPIRATION;
            case PASSWORD_EXPIRED:
                return DomibusMessageCode.SEC_PASSWORD_EXPIRED;

            default:
                throw new IllegalStateException("SecurityMessageCode for event type " + eventType + " not defined");
        }
    }
}
