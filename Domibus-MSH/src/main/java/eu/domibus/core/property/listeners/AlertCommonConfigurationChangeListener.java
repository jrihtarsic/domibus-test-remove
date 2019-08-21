package eu.domibus.core.property.listeners;

import eu.domibus.core.alerts.service.MultiDomainAlertConfigurationService;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Handles the change of alert properties that are related to common configuration
 */
@Service
public class AlertCommonConfigurationChangeListener implements PluginPropertyChangeListener {

    @Autowired
    private MultiDomainAlertConfigurationService multiDomainAlertConfigurationService;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsAnyIgnoreCase(propertyName,
                DOMIBUS_ALERT_MAIL_SENDING_ACTIVE,
                DOMIBUS_ALERT_SUPER_MAIL_SENDING_ACTIVE,
                DOMIBUS_ALERT_SENDER_EMAIL, DOMIBUS_ALERT_SUPER_SENDER_EMAIL,
                DOMIBUS_ALERT_RECEIVER_EMAIL, DOMIBUS_ALERT_SUPER_RECEIVER_EMAIL,
                DOMIBUS_ALERT_CLEANER_ALERT_LIFETIME,
                DOMIBUS_ALERT_SUPER_CLEANER_ALERT_LIFETIME);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        multiDomainAlertConfigurationService.clearCommonConfiguration();
    }

}
