package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusProperty;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.property.DomibusPropertyService;
import eu.domibus.web.rest.ro.DomibusPropertyRO;
import eu.domibus.web.rest.ro.PropertyFilterRequestRO;
import eu.domibus.web.rest.ro.PropertyResponseRO;
import eu.domibus.web.rest.validators.SkipWhiteListed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Resource responsible for getting the domibus properties that can be changed at runtime, getting and setting their values through REST Api
 */
@RestController
@RequestMapping(value = "/rest/configuration")
@Validated
public class PropertyResource {

    @Autowired
    private DomibusPropertyService domibusPropertyService;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    private DomainCoreConverter domainConverter;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AP_ADMIN')")
    @GetMapping(path = "/properties")
    public PropertyResponseRO getProperties(@Valid PropertyFilterRequestRO request) {
        PropertyResponseRO response = new PropertyResponseRO();
        List<DomibusProperty> items = domibusPropertyService.getProperties(request.getName());
        response.setCount(items.size());
        items = items.stream()
                .skip((long) request.getPage() * request.getPageSize())
                .limit(request.getPageSize())
                .collect(Collectors.toList());
        response.setItems(domainConverter.convert(items, DomibusPropertyRO.class));
        return response;
    }

    @PreAuthorize("hasAnyRole('ROLE_AP_ADMIN')")
    @GetMapping(path = "/superProperties")
    public PropertyResponseRO getProperties(@Valid PropertyFilterRequestRO request, String domain) {
        if (StringUtils.isEmpty(domain)) {
            domainContextProvider.clearCurrentDomain();
        } else
            domainContextProvider.setCurrentDomain(domain);

        return this.getProperties(request);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AP_ADMIN')")
    @PutMapping(path = "/properties/{propertyName:.+}")
    @SkipWhiteListed
    public void setProperty(@PathVariable String propertyName, @RequestBody String propertyValue) {
        domibusPropertyService.setPropertyValue(propertyName, propertyValue);
    }

    @PreAuthorize("hasAnyRole('ROLE_AP_ADMIN')")
    @PutMapping(path = "/superProperties/{propertyName:.+}")
    @SkipWhiteListed
    public void setProperty(@PathVariable String propertyName, @RequestBody String propertyValue, String domain) {
        if (StringUtils.isEmpty(domain)) {
            domainContextProvider.clearCurrentDomain();
        } else {
            domainContextProvider.setCurrentDomain(domain);
        }

        this.setProperty(propertyName, propertyValue);
    }
}
