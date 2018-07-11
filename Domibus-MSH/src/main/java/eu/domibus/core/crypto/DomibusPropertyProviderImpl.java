package eu.domibus.core.crypto;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.property.PropertyResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class DomibusPropertyProviderImpl implements DomibusPropertyProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusPropertyProviderImpl.class);

    @Autowired
    @Qualifier("domibusProperties")
    protected Properties domibusProperties;

    @Autowired
    protected PropertyResolver propertyResolver;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    protected String getPropertyName(Domain domain, String propertyName) {
        return domain.getCode() + "." + propertyName;
    }

    @Override
    public String getProperty(Domain domain, String propertyName) {
        final String domainPropertyName = getPropertyName(domain, propertyName);
        String propertyValue = domibusProperties.getProperty(domainPropertyName);
        if (StringUtils.isEmpty(propertyValue) && DomainService.DEFAULT_DOMAIN.equals(domain)) {
            propertyValue = domibusProperties.getProperty(propertyName);
        }
        return propertyValue;
    }


    @Override
    public String getProperty(Domain domain, String propertyName, String defaultValue) {
        String propertyValue = getProperty(domain, propertyName);
        if (StringUtils.isEmpty(propertyValue)) {
            propertyValue = defaultValue;
        }
        return propertyValue;
    }

    @Override
    public String getProperty(String propertyName) {
        return getProperty(DomainService.DEFAULT_DOMAIN, propertyName);
    }

    @Override
    public String getProperty(String propertyName, String defaultValue) {
        String propertyValue = getProperty(propertyName);
        if (StringUtils.isEmpty(propertyValue)) {
            propertyValue = defaultValue;
        }
        return propertyValue;
    }

    @Override
    public String getResolvedProperty(Domain domain, String propertyName) {
        final String domainPropertyName = getPropertyName(domain, propertyName);
        String resolvedProperty = propertyResolver.getResolvedProperty(domainPropertyName, domibusProperties, true);
        if (StringUtils.isEmpty(resolvedProperty) && DomainService.DEFAULT_DOMAIN.equals(domain)) {
            resolvedProperty = propertyResolver.getResolvedProperty(propertyName, domibusProperties, true);
        }
        return resolvedProperty;
    }

    @Override
    public String getResolvedProperty(String propertyName) {
        return getResolvedProperty(DomainService.DEFAULT_DOMAIN, propertyName);
    }


    @Override
    public String getDomainProperty(String propertyName) {
        final Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        assert currentDomain != null;
        return getDomainProperty(currentDomain, propertyName);
    }

    @Override
    public String getDomainProperty(String propertyName, String defaultValue) {
        String propertyValue = getDomainProperty(propertyName);
        if (StringUtils.isEmpty(propertyValue)) {
            propertyValue = defaultValue;
        }
        return propertyValue;
    }

    @Override
    public String getDomainProperty(Domain domain, String propertyName) {
        String propertyValue = getProperty(domain, propertyName);
        if (StringUtils.isEmpty(propertyValue) && !DomainService.DEFAULT_DOMAIN.equals(domain)) {
            propertyValue = getProperty(DomainService.DEFAULT_DOMAIN, propertyName);
        }
        return propertyValue;
    }

    @Override
    public String getDomainProperty(Domain domain, String propertyName, String defaultValue) {
        String propertyValue = getDomainProperty(domain, propertyName);
        if (StringUtils.isEmpty(propertyValue)) {
            propertyValue = defaultValue;
        }
        return propertyValue;
    }

}
