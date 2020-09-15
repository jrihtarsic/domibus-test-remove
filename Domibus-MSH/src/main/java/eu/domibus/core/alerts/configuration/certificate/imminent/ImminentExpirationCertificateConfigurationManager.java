package eu.domibus.core.alerts.configuration.certificate.imminent;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.AbstractAlertConfigurationManagerWithReaderMethod;
import eu.domibus.core.alerts.configuration.AlertConfigurationManager;
import eu.domibus.core.alerts.model.common.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.AlertConfigurationService;
import eu.domibus.core.alerts.service.ConfigurationReader;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * Manages the reading of certificate imminent expiration alert configuration
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class ImminentExpirationCertificateConfigurationManager
        extends AbstractAlertConfigurationManagerWithReaderMethod<ImminentExpirationCertificateModuleConfiguration>
        implements AlertConfigurationManager {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(ImminentExpirationCertificateConfigurationManager.class);

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    AlertConfigurationService alertConfigurationService;

    @Override
    public AlertType getAlertType() {
        return AlertType.CERT_IMMINENT_EXPIRATION;
    }

    @Override
    protected ConfigurationReader<ImminentExpirationCertificateModuleConfiguration> getReaderMethod() {
        return this::readConfiguration;
    }

    protected ImminentExpirationCertificateModuleConfiguration readConfiguration() {
        Domain domain = domainContextProvider.getCurrentDomainSafely();
        try {
            final Boolean alertActive = alertConfigurationService.isAlertModuleEnabled();
            final Boolean imminentExpirationActive = domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_ACTIVE);
            if (!alertActive || !imminentExpirationActive) {
                LOG.debug("domain:[{}] Alert certificate imminent expiration module is inactive for the following reason:global alert module active[{}], certificate imminent expiration module active[{}]",
                        domain, alertActive, imminentExpirationActive);
                return new ImminentExpirationCertificateModuleConfiguration();
            }
            final Integer imminentExpirationDelay = domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_DELAY_DAYS);
            final Integer imminentExpirationFrequency = domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_FREQUENCY_DAYS);
            final AlertLevel imminentExpirationAlertLevel = AlertLevel.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_LEVEL));
            final String imminentExpirationMailSubject = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_MAIL_SUBJECT);

            LOG.info("Alert certificate imminent expiration module activated for domain:[{}]", domain);
            return new ImminentExpirationCertificateModuleConfiguration(
                    imminentExpirationDelay,
                    imminentExpirationFrequency,
                    imminentExpirationAlertLevel,
                    imminentExpirationMailSubject);

        } catch (Exception e) {
            LOG.warn("An error occurred while reading certificate scanner alert module configuration for domain:[{}], ", domain, e);
            return new ImminentExpirationCertificateModuleConfiguration();
        }

    }

}
