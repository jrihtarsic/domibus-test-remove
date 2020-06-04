package eu.domibus.core.alerts.configuration.model;

import eu.domibus.core.alerts.model.common.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public class LoginFailureModuleConfiguration extends AlertModuleConfigurationBase {

    public LoginFailureModuleConfiguration(AlertType alertType) {
        super(alertType);
    }

    public LoginFailureModuleConfiguration(AlertType alertType, AlertLevel loginFailureAlertLevel, String loginFailureMailSubject) {
        super(alertType, loginFailureAlertLevel, loginFailureMailSubject);
    }

}
