package eu.domibus.core.property;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusProperty;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.ext.delegate.converter.DomainExtConverter;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Service called from the ConfigurationPropertyResource REST class
 * responsible with getting the domibus properties that can be changed at runtime, getting and setting their values
 */
@Service
public class ConfigurationPropertyServiceImpl implements ConfigurationPropertyService {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(ConfigurationPropertyServiceImpl.class);

    @Autowired
    protected DomainExtConverter domainConverter;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    /**
     * We inject here all managers: one for each plugin + domibus property manager delegate( which adapts DomibusPropertyManager to DomibusPropertyManagerExt)
     */
    @Autowired
    private List<DomibusPropertyManagerExt> propertyManagers;

    public List<DomibusProperty> getProperties(String name) {
        List<DomibusProperty> list = new ArrayList<>();

        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        String domainCode = currentDomain == null ? null : currentDomain.getCode();

        for (DomibusPropertyManagerExt propertyManager : propertyManagers) {
            List<DomibusPropertyMetadataDTO> knownProps = propertyManager.getKnownProperties().values().stream()
                    .filter(p -> p.isWritable())
                    .filter(p -> name == null || p.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());

            if (domibusConfigurationService.isMultiTenantAware()) {
                if (currentDomain == null) {
                    knownProps = knownProps.stream().filter(p -> p.isGlobal()).collect(Collectors.toList());
                } else {
                    knownProps = knownProps.stream().filter(p -> p.getType() == DomibusPropertyMetadataDTO.Type.DOMAIN).collect(Collectors.toList());
                }
            }

            for (DomibusPropertyMetadataDTO p : knownProps) {
                String value = propertyManager.getKnownPropertyValue(domainCode, p.getName());
                DomibusPropertyMetadata meta = domainConverter.convert(p, DomibusPropertyMetadata.class);

                DomibusProperty prop = new DomibusProperty();
                prop.setMetadata(meta);
                prop.setValue(value);

                list.add(prop);
            }
        }

        return list;
    }

    @Transactional(noRollbackFor = DomibusCoreException.class)
    public void setPropertyValue(String name, String value) {
        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        String domainCode = currentDomain == null ? null : currentDomain.getCode();

        boolean handled = false;
        for (DomibusPropertyManagerExt propertyManager : propertyManagers) {
            if (propertyManager.hasKnownProperty(name)) {
                propertyManager.setKnownPropertyValue(domainCode, name, value);
                handled = true;
            }
        }

        if (!handled) {
            LOG.debug("Property manager not found for [{}]", name);
            throw new IllegalArgumentException("Property not found: " + name);
        }
    }

}
