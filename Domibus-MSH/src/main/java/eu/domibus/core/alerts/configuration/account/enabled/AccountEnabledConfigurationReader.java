package eu.domibus.core.alerts.configuration.account.enabled;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.AlertConfigurationReader;
import eu.domibus.core.alerts.configuration.AlertModuleConfigurationBase;
import eu.domibus.core.alerts.model.common.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.AlertConfigurationService;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base code for reading of console and plugin user account enabled alert configuration
 *
 * @author Ion Perpegel
 * @since 4.2
 */
public abstract class AccountEnabledConfigurationReader
        implements AlertConfigurationReader<AlertModuleConfigurationBase> {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(AccountEnabledConfigurationReader.class);

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    AlertConfigurationService alertConfigurationService;

    @Override
    public abstract AlertType getAlertType();

    protected abstract String getModuleName();

    protected abstract String getAlertActivePropertyName();

    protected abstract String getAlertLevelPropertyName();

    protected abstract String getAlertEmailSubjectPropertyName();

    public AlertModuleConfigurationBase readConfiguration() {

        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        try {
            final Boolean alertActive = alertConfigurationService.isAlertModuleEnabled();
            final Boolean accountEnabledActive = domibusPropertyProvider.getBooleanProperty(getAlertActivePropertyName());
            if (!alertActive || !accountEnabledActive) {
                LOG.debug("domain:[{}] [{}] module is inactive for the following reason: global alert module active:[{}], account disabled module active:[{}]"
                        , currentDomain, getModuleName(), alertActive, accountEnabledActive);
                return new AlertModuleConfigurationBase(getAlertType());
            }

            final AlertLevel level = AlertLevel.valueOf(domibusPropertyProvider.getProperty(getAlertLevelPropertyName()));
            final String mailSubject = domibusPropertyProvider.getProperty(getAlertEmailSubjectPropertyName());

            LOG.info("[{}] module activated for domain:[{}]", getModuleName(), currentDomain);
            return new AlertModuleConfigurationBase(getAlertType(), level, mailSubject);

        } catch (Exception e) {
            LOG.warn("An error occurred while reading [{}] module configuration for domain:[{}], ", getModuleName(), currentDomain, e);
            return new AlertModuleConfigurationBase(getAlertType());
        }
    }

}
