package eu.domibus.core.alerts.configuration.manager;

import eu.domibus.core.alerts.configuration.reader.PluginLoginFailConfigurationReader;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.ConfigurationLoader;
import eu.domibus.core.alerts.configuration.model.LoginFailureModuleConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manages the reading of plugin user login fail alert configuration
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class PluginLoginFailConfigurationManager implements AlertConfigurationManager {

    @Autowired
    private PluginLoginFailConfigurationReader reader;

    @Autowired
    private ConfigurationLoader<LoginFailureModuleConfiguration> loader;

    @Override
    public AlertType getAlertType() {
        return reader.getAlertType();
    }

    @Override
    public LoginFailureModuleConfiguration getConfiguration() {
        return loader.getConfiguration(reader::readConfiguration);
    }

    @Override
    public void reset() {
        loader.resetConfiguration();
    }
}
