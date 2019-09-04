package eu.domibus.core.alerts.service;

import eu.domibus.api.user.UserBase;
import eu.domibus.common.dao.security.UserDaoBase;
import eu.domibus.common.model.security.UserEntityBase;
import eu.domibus.common.model.security.UserLoginErrorReason;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.AccountDisabledModuleConfiguration;
import eu.domibus.core.alerts.model.service.LoginFailureModuleConfiguration;
import eu.domibus.core.security.AuthenticationDAO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION;
import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@Service
public class PluginUserAlertsServiceImpl extends UserAlertsServiceImpl {

    protected final static String MAXIMUM_PASSWORD_AGE = DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION; //NOSONAR
    protected final static String MAXIMUM_DEFAULT_PASSWORD_AGE = DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION; //NOSONAR

    @Autowired
    protected AuthenticationDAO userDao;

    @Autowired
    private MultiDomainAlertConfigurationService alertConfiguration;

    @Autowired
    private EventService eventService;

    @Autowired
    private MultiDomainAlertConfigurationService alertsConfiguration;


    @Override
    protected String getMaximumDefaultPasswordAgeProperty() {
        return MAXIMUM_DEFAULT_PASSWORD_AGE;
    }

    @Override
    protected String getMaximumPasswordAgeProperty() {
        return MAXIMUM_PASSWORD_AGE;
    }

    @Override
    protected AlertType getAlertTypeForPasswordImminentExpiration() {
        return AlertType.PLUGIN_PASSWORD_IMMINENT_EXPIRATION;
    }

    @Override
    protected AlertType getAlertTypeForPasswordExpired() {
        return AlertType.PLUGIN_PASSWORD_EXPIRED;
    }

    @Override
    protected EventType getEventTypeForPasswordImminentExpiration() {
        return EventType.PLUGIN_PASSWORD_IMMINENT_EXPIRATION;
    }

    @Override
    protected EventType getEventTypeForPasswordExpired() {
        return EventType.PLUGIN_PASSWORD_EXPIRED;
    }

    @Override
    protected UserDaoBase getUserDao() {
        return userDao;
    }

    @Override
    protected UserEntityBase.Type getUserType() {
        return UserEntityBase.Type.PLUGIN;
    }

    @Override
    protected AccountDisabledModuleConfiguration getAccountDisabledConfiguration() {
        return alertsConfiguration.getPluginAccountDisabledConfiguration();
    }

    @Override
    protected LoginFailureModuleConfiguration getLoginFailureConfiguration() {
        return alertsConfiguration.getPluginLoginFailureConfiguration();
    }

}
