package eu.domibus.configuration;

import eu.domibus.api.configuration.DataBaseEngine;
import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_DIALECT;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS)
public class DefaultDomibusConfigurationService implements DomibusConfigurationService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DefaultDomibusConfigurationService.class);

    private static final String DATABASE_DIALECT = DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_DIALECT;

    private DataBaseEngine dataBaseEngine;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Override
    public String getConfigLocation() {
        return System.getProperty(DOMIBUS_CONFIG_LOCATION);
    }

    @Cacheable(value = "multitenantCache")
    @Override
    public boolean isMultiTenantAware() {
        return StringUtils.isNotBlank(domibusPropertyProvider.getProperty(DomainService.GENERAL_SCHEMA_PROPERTY));
    }

    @Override
    public boolean isClusterDeployment() {
        return domibusPropertyProvider.getBooleanProperty(CLUSTER_DEPLOYMENT);
    }

    @Override
    public DataBaseEngine getDataBaseEngine() {
        if (dataBaseEngine == null) {
            final String property = domibusPropertyProvider.getProperty(DATABASE_DIALECT);
            if (property == null) {
                throw new IllegalStateException("Database dialect not configured, please set property: domibus.entityManagerFactory.jpaProperty.hibernate.dialect");
            }
            dataBaseEngine = DataBaseEngine.getDatabaseEngine(property);
            LOG.debug("Database engine:[{}]", dataBaseEngine);
        }
        return dataBaseEngine;
    }

    @Override
    public boolean isFourCornerEnabled() {
        return domibusPropertyProvider.getBooleanProperty(FOURCORNERMODEL_ENABLED_KEY);
    }

    @Override
    public boolean isExtAuthProviderEnabled() {
        return domibusPropertyProvider.getBooleanProperty(EXTERNAL_AUTH_PROVIDER);
    }

    @Override
    public boolean isPayloadEncryptionActive(Domain domain) {
        return domibusPropertyProvider.getBooleanProperty(domain, PAYLOAD_ENCRYPTION_PROPERTY);
    }

    @Override
    public boolean isPasswordEncryptionActive() {
        return domibusPropertyProvider.getBooleanProperty(PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
    }

    @Override
    public boolean isPasswordEncryptionActive(Domain domain) {
        return domibusPropertyProvider.getBooleanProperty(domain, PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
    }

    @Override
    public String getConfigurationFileName() {
        return DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
    }

    @Override
    public String getConfigurationFileName(Domain domain) {
        String propertyFileName = null;
        if (DomainService.DEFAULT_DOMAIN.equals(domain)) {
            final String configurationFile = getConfigLocation() + File.separator + getDomainConfigurationFileName(DomainService.DEFAULT_DOMAIN);
            LOG.debug("Checking if file [{}] exists", configurationFile);
            if (new File(configurationFile).exists()) {
                LOG.debug("Using property file [{}]", configurationFile);
                propertyFileName = configurationFile;
            } else {
                LOG.debug("File [{}] does not exists, using [{}]", configurationFile, DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE);
                propertyFileName = DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
            }
        } else {
            propertyFileName = getDomainConfigurationFileName(domain);
            LOG.debug("Using property file [{}]", propertyFileName);
        }

        return propertyFileName;
    }

    protected String getDomainConfigurationFileName(Domain domain) {
        return domain.getCode() + "-" + DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
    }


}
