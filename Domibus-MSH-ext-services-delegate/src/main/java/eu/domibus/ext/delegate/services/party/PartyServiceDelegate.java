package eu.domibus.ext.delegate.services.party;

import eu.domibus.api.party.Party;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.process.Process;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.ext.delegate.converter.DomainExtConverter;
import eu.domibus.ext.domain.PartyDTO;
import eu.domibus.ext.domain.ProcessDTO;
import eu.domibus.ext.domain.TrustStoreDTO;
import eu.domibus.ext.exceptions.PartyExtServiceException;
import eu.domibus.ext.services.PartyExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyStoreException;
import java.util.List;

/**
 * {@inheritDoc}
 * @since 4.2
 * @author Catalin Enache
 */
@Service
public class PartyServiceDelegate implements PartyExtService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyServiceDelegate.class);

    @Autowired
    PartyService partyService;

    @Autowired
    CertificateService certificateService;

    @Autowired
    DomainExtConverter domainConverter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createParty(PartyDTO partyDTO) {
        Party newParty = domainConverter.convert(partyDTO, Party.class);

        partyService.createParty(newParty, partyDTO.getCertificateContent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PartyDTO> getParties(String name,
                                     String endPoint,
                                     String partyId,
                                     String processName,
                                     int pageStart,
                                     int pageSize) {

        List<Party> parties = partyService.getParties(name, endPoint, partyId, processName, pageStart, pageSize);

        LOG.debug("Returned [{}] parties", parties != null ? parties.size() : 0);
        return domainConverter.convert(parties, PartyDTO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateParty(PartyDTO partyDTO) {
        //first we delete the party
        final String partyName = partyDTO.getName();
        deleteParty(partyName);

        //then we create it
        partyDTO.setEntityId(0);
        createParty(partyDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteParty(String partyName) {
        partyService.deleteParty(partyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrustStoreDTO getPartyCertificateFromTruststore(String partyName)  {

        TrustStoreEntry trustStoreEntry;
        try {
            trustStoreEntry = certificateService.getPartyCertificateFromTruststore(partyName);
        } catch (KeyStoreException e) {
            LOG.error("getPartyCertificateFromTruststore returned exception", e);
            throw new PartyExtServiceException(e);
        }
        LOG.debug("Returned trustStoreEntry=[{}] for party name=[{}]", trustStoreEntry.getName(), partyName);
        return domainConverter.convert(trustStoreEntry, TrustStoreDTO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProcessDTO> getAllProcesses() {
        List<Process> processList = partyService.getAllProcesses();
        LOG.debug("Returned [{}] processes", processList != null ? processList.size() : 0);
        return domainConverter.convert(processList, ProcessDTO.class);
    }
}
