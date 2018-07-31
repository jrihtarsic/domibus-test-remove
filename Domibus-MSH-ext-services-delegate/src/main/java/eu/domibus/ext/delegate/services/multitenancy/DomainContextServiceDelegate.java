package eu.domibus.ext.delegate.services.multitenancy;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.ext.delegate.converter.DomainExtConverter;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class DomainContextServiceDelegate implements DomainContextExtService {

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomainExtConverter domainConverter;

    @Override
    public DomainDTO getCurrentDomain() {
        final Domain currentDomain = domainContextProvider.getCurrentDomain();
        return domainConverter.convert(currentDomain, DomainDTO.class);
    }

    @Override
    public DomainDTO getCurrentDomainSafely() {
        final Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        return domainConverter.convert(currentDomain, DomainDTO.class);
    }

    @Override
    public void setCurrentDomain(DomainDTO domainDTO) {
        final Domain domain = domainConverter.convert(domainDTO, Domain.class);
        domainContextProvider.setCurrentDomain(domain);
    }

    @Override
    public void clearCurrentDomain() {
        domainContextProvider.clearCurrentDomain();
    }
}
