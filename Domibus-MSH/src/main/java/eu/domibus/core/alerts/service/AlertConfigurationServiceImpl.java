package eu.domibus.core.alerts.service;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.AlertConfigurationManager;
import eu.domibus.core.alerts.configuration.AlertModuleConfiguration;
import eu.domibus.core.alerts.configuration.AlertModuleConfigurationBase;
import eu.domibus.core.alerts.configuration.account.disabled.AccountDisabledModuleConfiguration;
import eu.domibus.core.alerts.configuration.account.disabled.ConsoleAccountDisabledConfigurationManager;
import eu.domibus.core.alerts.configuration.account.disabled.PluginAccountDisabledConfigurationManager;
import eu.domibus.core.alerts.configuration.account.enabled.ConsoleAccountEnabledConfigurationManager;
import eu.domibus.core.alerts.configuration.account.enabled.PluginAccountEnabledConfigurationManager;
import eu.domibus.core.alerts.configuration.certificate.ExpiredCertificateConfigurationManager;
import eu.domibus.core.alerts.configuration.certificate.ExpiredCertificateModuleConfiguration;
import eu.domibus.core.alerts.configuration.certificate.ImminentExpirationCertificateConfigurationManager;
import eu.domibus.core.alerts.configuration.certificate.ImminentExpirationCertificateModuleConfiguration;
import eu.domibus.core.alerts.configuration.common.CommonConfiguration;
import eu.domibus.core.alerts.configuration.common.CommonConfigurationManager;
import eu.domibus.core.alerts.configuration.login.ConsoleLoginFailConfigurationManager;
import eu.domibus.core.alerts.configuration.login.LoginFailureModuleConfiguration;
import eu.domibus.core.alerts.configuration.login.PluginLoginFailConfigurationManager;
import eu.domibus.core.alerts.configuration.messaging.MessagingConfigurationManager;
import eu.domibus.core.alerts.configuration.messaging.MessagingModuleConfiguration;
import eu.domibus.core.alerts.configuration.password.*;
import eu.domibus.core.alerts.configuration.password.expired.ConsolePasswordExpiredAlertConfigurationManager;
import eu.domibus.core.alerts.configuration.password.expired.PluginPasswordExpiredAlertConfigurationManager;
import eu.domibus.core.alerts.configuration.password.imminent.ConsolePasswordImminentExpirationAlertConfigurationManager;
import eu.domibus.core.alerts.configuration.password.imminent.PluginPasswordImminentExpirationAlertConfigurationManager;
import eu.domibus.core.alerts.model.common.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author Thomas Dussart, Ion Perpegel
 * @since 4.0
 */
@Service
public class AlertConfigurationServiceImpl implements AlertConfigurationService {
    private static final Logger LOG = DomibusLoggerFactory.getLogger(AlertConfigurationServiceImpl.class);

    static final String DOMIBUS_ALERT_SUPER_INSTANCE_NAME_SUBJECT = DOMIBUS_INSTANCE_NAME;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    List<AlertConfigurationManager> alertConfigurationManagers;

    @Autowired
    private CommonConfigurationManager commonConfigurationManager;

    @Autowired
    private PluginPasswordImminentExpirationAlertConfigurationManager pluginPasswordImminentExpirationAlertConfigurationManager;

    @Override
    public PasswordExpirationAlertModuleConfiguration getPluginPasswordImminentExpirationAlertConfigurationManager() {
        return pluginPasswordImminentExpirationAlertConfigurationManager.getConfiguration();
    }

    @Override
    public void clearPluginPasswordImminentExpirationAlertConfigurationManager() {
        pluginPasswordImminentExpirationAlertConfigurationManager.reset();
    }

    @Override
    public void clearAllConfigurations() {
        commonConfigurationManager.reset();
        Arrays.asList(AlertType.values()).forEach(alertType -> getModuleConfigurationManager(alertType).reset());
    }

    @Override
    public AlertLevel getAlertLevel(Alert alert) {
        return getModuleConfiguration(alert.getAlertType()).getAlertLevel(alert);
    }

    @Override
    public String getMailSubject(AlertType alertType) {
        return getModuleConfiguration(alertType).getMailSubject();
    }

    @Override
    public Boolean isAlertModuleEnabled() {
        return domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_ACTIVE);
    }

    @Override
    public String getSendEmailActivePropertyName() {
        return DOMIBUS_ALERT_MAIL_SENDING_ACTIVE;
    }

    @Override
    public String getAlertRetryMaxAttemptPropertyName() {
        return DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS;
    }

    @Override
    public String getAlertRetryTimePropertyName() {
        return DOMIBUS_ALERT_RETRY_TIME;
    }

    @Override
    public String getAlertSuperServerNameSubjectPropertyName() {
        return DOMIBUS_ALERT_SUPER_INSTANCE_NAME_SUBJECT;
    }

    protected AlertModuleConfiguration getModuleConfiguration(AlertType alertType) {
        return getModuleConfigurationManager(alertType).getConfiguration();
    }

    protected AlertConfigurationManager getModuleConfigurationManager(AlertType alertType) {
        Optional<AlertConfigurationManager> res = alertConfigurationManagers.stream().filter(el -> el.getAlertType() == alertType).findFirst();
        if (!res.isPresent()) {
            throw new IllegalArgumentException("Invalid alert type");
        }
        return res.get();
    }
}
