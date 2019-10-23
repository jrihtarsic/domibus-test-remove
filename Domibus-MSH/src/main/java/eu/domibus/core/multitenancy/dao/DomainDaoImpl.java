package eu.domibus.core.multitenancy.dao;


import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.services.DomibusCacheService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMAIN_TITLE;


/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Component
public class DomainDaoImpl implements DomainDao {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomainDaoImpl.class);

    private static final String[] DOMAIN_FILE_EXTENSION = {"properties"};
    private static final String DOMAIN_FILE_SUFFIX = "-domibus";

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Cacheable(value = DomibusCacheService.ALL_DOMAINS_CACHE)
    @Override
    public List<Domain> findAll() {
        LOG.trace("Finding all domains");

        List<Domain> result = new ArrayList<>();
        result.add(DomainService.DEFAULT_DOMAIN);
        if (!domibusConfigurationService.isMultiTenantAware()) {
            LOG.trace("Domibus is running in non multitenant mode, adding only the default domain");
            return result;
        }

        final String propertyValue = domibusConfigurationService.getConfigLocation();
        File confDirectory = new File(propertyValue);
        final Collection<File> propertyFiles = FileUtils.listFiles(confDirectory, DOMAIN_FILE_EXTENSION, false);

        if (propertyFiles == null) {
            LOG.trace("Could not find any files with extension [{}] in directory [{}]", DOMAIN_FILE_EXTENSION, confDirectory);
            return result;
        }

        List<Domain> additionalDomains = new ArrayList<>();
        for (File propertyFile : propertyFiles) {
            final String fileName = propertyFile.getName();
            if (StringUtils.containsIgnoreCase(fileName, DOMAIN_FILE_SUFFIX)) {
                LOG.trace("Getting domain code from file [{}]", fileName);
                String domainCode = StringUtils.substringBefore(fileName, DOMAIN_FILE_SUFFIX);
                if (StringUtils.equalsAnyIgnoreCase(domainCode, "default", "super")) {
                    // "default" and "super" are not additional domains, but regular ones
                    continue;
                }

                Domain domain = new Domain(domainCode, null);
                domain.setName(getDomainTitle(domain));
                additionalDomains.add(domain);

                LOG.trace("Added domain [{}]", domain);
            }
        }
        additionalDomains.sort(Comparator.comparing(Domain::getName, String.CASE_INSENSITIVE_ORDER));
        result.addAll(additionalDomains);

        LOG.trace("Found the following domains [{}]", result);

        return result;
    }

    protected String getDomainTitle(Domain domain) {
        String domainTitle = domibusPropertyProvider.getProperty(domain, DOMAIN_TITLE);
        if (StringUtils.isEmpty(domainTitle)) {
            domainTitle = domain.getCode();
        }
        return domainTitle;
    }
}

