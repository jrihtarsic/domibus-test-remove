package eu.domibus.core.alerts.configuration.account.disabled.plugin;

import eu.domibus.core.alerts.configuration.AlertConfigurationManager;
import eu.domibus.core.alerts.configuration.account.disabled.AccountDisabledModuleConfiguration;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.ConfigurationLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manages the reading of plugin user account disabled alert configuration
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class PluginAccountDisabledConfigurationManager implements AlertConfigurationManager {

    @Autowired
    private PluginAccountDisabledConfigurationReader reader;

    @Autowired
    private ConfigurationLoader<AccountDisabledModuleConfiguration> loader;

    @Override
    public AlertType getAlertType() {
        return reader.getAlertType();
    }

    @Override
    public AccountDisabledModuleConfiguration getConfiguration() {
        return loader.getConfiguration(reader::readConfiguration);
    }

    @Override
    public void reset() {
        loader.resetConfiguration();
    }
}