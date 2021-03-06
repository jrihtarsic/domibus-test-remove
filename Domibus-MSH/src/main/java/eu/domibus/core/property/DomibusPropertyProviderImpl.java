package eu.domibus.core.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Cosmin Baciu, Ion Perpegel
 * @since 4.0
 */
@Service
public class DomibusPropertyProviderImpl implements DomibusPropertyProvider {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(DomibusPropertyProviderImpl.class);

    @Autowired
    @Qualifier("domibusProperties")
    protected Properties domibusProperties;

    @Autowired
    @Qualifier("domibusDefaultProperties")
    protected Properties domibusDefaultProperties;

    @Autowired
    protected PropertyResolver propertyResolver;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected PasswordEncryptionService passwordEncryptionService;

    protected String getPropertyName(Domain domain, String propertyName) {
        return domain.getCode() + "." + propertyName;
    }

    @Override
    public String getProperty(Domain domain, String propertyName) {
        return getProperty(domain, propertyName, false);
    }

    @Override
    public String getProperty(Domain domain, String propertyName, boolean decrypt) {
        final String domainPropertyName = getPropertyName(domain, propertyName);
        String propertyValue = getPropertyValue(domainPropertyName, domain, decrypt);
        if (StringUtils.isEmpty(propertyValue) && DomainService.DEFAULT_DOMAIN.equals(domain)) {
            propertyValue = getPropertyValue(propertyName, domain, decrypt);
        }

        return propertyValue;
    }

    /**
     * Get the value from the system environment properties; if not found get the value from the system properties; if not found get the value from Domibus properties;
     * if still not found, look inside the Domibus default properties.
     *
     * @param propertyName the property name
     * @return The value of the property as found in the system properties, the Domibus properties or inside the default Domibus properties.
     */
    protected String getPropertyValue(String propertyName, Domain domain, boolean decrypt) {
        String result = System.getenv(propertyName);
        if (StringUtils.isEmpty(result)) {
            result = System.getProperty(propertyName);
        }
        if (StringUtils.isEmpty(result)) {
            result = domibusProperties.getProperty(propertyName);

            // There is no need to retrieve the default Domibus property value here since the Domibus properties above will contain it, unless overwritten by users.
            // For String property values, if users have overwritten their original default Domibus property values, it is their responsibility to ensure they are valid.
            // For all the other Boolean and Integer property values, if users have overwritten their original default Domibus property values, they are defaulted back to their
            // original default Domibus values when invalid (please check the #getInteger..(..) and #getBoolean..(..) methods below).
        }
        if (StringUtils.contains(result, "${")) {
            LOGGER.debug("Resolving property [{}]", propertyName);
            result = propertyResolver.getResolvedValue(result, domibusProperties, true);
        }
        if (decrypt && passwordEncryptionService.isValueEncrypted(result)) {
            LOGGER.debug("Decrypting property [{}]", propertyName);
            result = passwordEncryptionService.decryptProperty(domain, propertyName, result);
        }

        return result;
    }

    @Override
    public String getProperty(String propertyName) {
        return getProperty(propertyName, false);
    }


    @Override
    public String getProperty(String propertyName, boolean decrypt) {
        return getPropertyValue(propertyName, null, decrypt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainProperty(String propertyName) {
        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        if (currentDomain == null) {
            currentDomain = DomainService.DEFAULT_DOMAIN;
        }
        return getDomainProperty(currentDomain, propertyName);
    }

    /**
     * Retrieves the property value from the requested domain.
     * If not found, fall back to the property value from the default domain.
     */
    @Override
    public String getDomainProperty(Domain domain, String propertyName) {
        String propertyValue = getProperty(domain, propertyName);
        if (StringUtils.isEmpty(propertyValue) && !DomainService.DEFAULT_DOMAIN.equals(domain)) {
            propertyValue = getProperty(DomainService.DEFAULT_DOMAIN, propertyName);
        }
        return propertyValue;
    }

    @Override
    public Set<String> filterPropertiesName(Predicate<String> predicate) {
        Set<String> filteredPropertyNames = new HashSet<>();
        final Enumeration<?> enumeration = domibusProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            final String propertyName = (String) enumeration.nextElement();
            if (predicate.test(propertyName)) {
                filteredPropertyNames.add(propertyName);
            }
        }
        return filteredPropertyNames;
    }

    @Override
    public Integer getIntegerProperty(String propertyName) {
        String value = getProperty(propertyName);
        return getIntegerInternal(propertyName, value);
    }

    @Override
    public Integer getIntegerDomainProperty(String propertyName) {
        String domainValue = getDomainProperty(propertyName);
        return getIntegerInternal(propertyName, domainValue);
    }

    @Override
    public Integer getIntegerDomainProperty(Domain domain, String propertyName) {
        String domainValue = getDomainProperty(domain, propertyName);
        return getIntegerInternal(propertyName, domainValue);
    }

    @Override
    public Long getLongDomainProperty(Domain domain, String propertyName) {
        String domainValue = getDomainProperty(domain, propertyName);
        return getLongInternal(propertyName, domainValue);
    }

    private Integer getIntegerInternal(String propertyName, String customValue) {
        if (customValue != null) {
            try {
                return Integer.valueOf(customValue);
            } catch (final NumberFormatException e) {
                LOGGER.warn("Could not parse the property [" + propertyName + "] custom value [" + customValue + "] to an integer value", e);
                return getDefaultIntegerValue(propertyName);
            }
        }
        return getDefaultIntegerValue(propertyName);
    }

    protected Long getLongInternal(String propertyName, String customValue) {
        if (customValue != null) {
            try {
                return Long.valueOf(customValue);
            } catch (final NumberFormatException e) {
                LOGGER.warn("Could not parse the property [" + propertyName + "] custom value [" + customValue + "] to a Long value", e);
                return getDefaultLongValue(propertyName);
            }
        }
        return getDefaultLongValue(propertyName);
    }

    protected Integer getDefaultIntegerValue(String propertyName) {
        Integer defaultValue = MapUtils.getInteger(domibusDefaultProperties, propertyName);
        return checkDefaultValue(propertyName, defaultValue);
    }

    protected Long getDefaultLongValue(String propertyName) {
        Long defaultValue = MapUtils.getLong(domibusDefaultProperties, propertyName);
        return checkDefaultValue(propertyName, defaultValue);
    }

    @Override
    public Boolean getBooleanProperty(String propertyName) {
        String value = getProperty(propertyName);
        return getBooleanInternal(propertyName, value);
    }

    @Override
    public Boolean getBooleanDomainProperty(String propertyName) {
        String domainValue = getDomainProperty(propertyName);
        return getBooleanInternal(propertyName, domainValue);
    }

    @Override
    public Boolean getBooleanDomainProperty(Domain domain, String propertyName) {
        String domainValue = getDomainProperty(domain, propertyName);
        return getBooleanInternal(propertyName, domainValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsDomainPropertyKey(Domain domain, String propertyName) {
        final String domainPropertyName = getPropertyName(domain, propertyName);
        boolean domainPropertyKeyFound = domibusProperties.containsKey(domainPropertyName);
        if (!domainPropertyKeyFound) {
            domainPropertyKeyFound = domibusProperties.containsKey(propertyName);
        }
        return domainPropertyKeyFound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsPropertyKey(String propertyName) {
        return domibusProperties.containsKey(propertyName);
    }

    private Boolean getBooleanInternal(String propertyName, String customValue) {
        if (customValue != null) {
            Boolean customBoolean = BooleanUtils.toBooleanObject(customValue);
            if (customBoolean != null) {
                return customBoolean;
            }
            LOGGER.warn("Could not parse the property [{}] custom value [{}] to a boolean value", propertyName, customValue);
            return getDefaultBooleanValue(propertyName);
        }
        return getDefaultBooleanValue(propertyName);
    }

    private Boolean getDefaultBooleanValue(String propertyName) {
        // We need to fetch the Boolean value in two steps as the MapUtils#getBoolean(Properties, String) does not return "null" when the value is an invalid Boolean.
        String defaultValue = MapUtils.getString(domibusDefaultProperties, propertyName);
        Boolean defaultBooleanValue = BooleanUtils.toBooleanObject(defaultValue);
        return checkDefaultValue(propertyName, defaultBooleanValue);
    }

    private <T> T checkDefaultValue(String propertyName, T defaultValue) {
        if (defaultValue == null) {
            throw new IllegalStateException("The default property [" + propertyName + "] is required but was either not found inside the default properties or found having an invalid value");
        }
        LOGGER.debug("Found the property [{}] default value [{}]", propertyName, defaultValue);
        return defaultValue;
    }

    @Override
    public void setPropertyValue(Domain domain, String propertyName, String propertyValue) {
        String propertyKey = propertyName;
        if (domain != null && !DomainService.DEFAULT_DOMAIN.equals(domain)) {
            propertyKey = getPropertyName(domain, propertyName);
        }
        this.domibusProperties.setProperty(propertyKey, propertyValue);
    }

}
