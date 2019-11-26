package eu.domibus.core.property.listeners;

import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.api.property.DomibusPropertyMetadataManager;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.MultiDomainAlertConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Handles the change of alert properties that are related to configuration of expired plugin password alerts
 */
@Service
public class AlertPluginPasswordExpiredConfigurationChangeListener implements DomibusPropertyChangeListener {

    @Autowired
    private MultiDomainAlertConfigurationService multiDomainAlertConfigurationService;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.startsWithIgnoreCase(propertyName, DomibusPropertyMetadataManager.DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_PREFIX);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        multiDomainAlertConfigurationService.clearPasswordExpirationAlertConfiguration(AlertType.PLUGIN_PASSWORD_EXPIRED);
    }
}

