package eu.domibus.core.alerts.model.service;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.core.alerts.service.ConfigurationReader;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Dussart
 * @since 4.0
 */

@Component
@Scope("prototype")
public class ConfigurationLoader<E> {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(ConfigurationLoader.class);
    public static final Domain SUPER_DOMAIN = new Domain("super_domain", "Super Domain"); // this is the placeholder domain used only as a caching key for super users

    @Autowired
    private DomainContextProvider domainContextProvider;

    private final Map<Domain, E> configuration = new HashMap<>();

    public E getConfiguration(ConfigurationReader<E> configurationReader) {
        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        final Domain key = currentDomain == null ? SUPER_DOMAIN : currentDomain;
        final Domain domain = currentDomain == null ? DomainService.DEFAULT_DOMAIN : currentDomain;
        LOG.debug("Retrieving alert messaging configuration for domain:[{}]", currentDomain);
        if (this.configuration.get(key) == null) {
            synchronized (this.configuration) {
                if (this.configuration.get(key) == null) {
                    this.configuration.put(key, configurationReader.readConfiguration(domain));
                }
            }
        }
        E result = this.configuration.get(key);
        LOG.debug("Alert messaging configuration:[{}]", result);
        return result;
    }

}



