package eu.domibus.common.services.impl;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.services.DynamicDiscoveryService;
import eu.domibus.common.util.DomibusApacheFetcher;
import eu.domibus.common.util.EndpointInfo;
import eu.domibus.common.util.ProxyUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.common.lang.PeppolParsingException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.lookup.locator.BusdoxLocator;
import no.difi.vefa.peppol.mode.Mode;
import no.difi.vefa.peppol.security.lang.PeppolSecurityException;
import no.difi.vefa.peppol.security.util.EmptyCertificateValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service to query the SMP to extract the required information about the unknown receiver AP.
 * The SMP Lookup is done using an SMP Client software, with the following input:
 *       The End Receiver Participant ID (C4)
 *       The Document ID
 *       The Process ID
 *
 * Upon a successful lookup, the result contains the endpoint address and also othe public certificate of the receiver.
 */
@Service
@Qualifier("dynamicDiscoveryServicePEPPOL")
public class DynamicDiscoveryServicePEPPOL implements DynamicDiscoveryService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DynamicDiscoveryServicePEPPOL.class);

    private static final String RESPONDER_ROLE = "urn:fdc:peppol.eu:2017:roles:ap:as4";
    private static final String PARTYID_TYPE = "urn:fdc:peppol.eu:2017:identifiers:ap";
    public static final String SCHEME_DELIMITER = "::";

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    protected ProxyUtil proxyUtil;

    @Cacheable(value = "lookupInfo", key = "#domain + #participantId + #participantIdScheme + #documentId + #processId + #processIdScheme")
    public EndpointInfo lookupInformation(final String domain, final String participantId, final String participantIdScheme, final String documentId, final String processId, final String processIdScheme) {

        LOG.info("[PEPPOL SMP] Do the lookup by: [{}] [{}] [{}] [{}] [{}]", participantId, participantIdScheme, documentId, processId, processIdScheme);
        final String smlInfo = domibusPropertyProvider.getDomainProperty(SMLZONE_KEY);
        if (smlInfo == null) {
            throw new ConfigurationException("SML Zone missing. Configure in domibus-configuration.xml");
        }
        String mode = domibusPropertyProvider.getDomainProperty(DYNAMIC_DISCOVERY_MODE, Mode.TEST);
        try {
            final LookupClientBuilder lookupClientBuilder = LookupClientBuilder.forMode(mode);
            lookupClientBuilder.locator(new BusdoxLocator(smlInfo));
            lookupClientBuilder.fetcher(new DomibusApacheFetcher(Mode.of(mode), domibusConfigurationService, proxyUtil));
            /* DifiCertificateValidator.validate fails when proxy is enabled */
            if(domibusConfigurationService.useProxy()) {
                lookupClientBuilder.certificateValidator(EmptyCertificateValidator.INSTANCE);
            }
            final LookupClient smpClient = lookupClientBuilder.build();
            final ParticipantIdentifier participantIdentifier = ParticipantIdentifier.of(participantId, Scheme.of(participantIdScheme));
            final DocumentTypeIdentifier documentIdentifier = getDocumentTypeIdentifier(documentId);

            final ProcessIdentifier processIdentifier = getProcessIdentifier(processId);
            LOG.debug("Getting the ServiceMetadata");
            final ServiceMetadata sm = smpClient.getServiceMetadata(participantIdentifier, documentIdentifier);

            String transportProfileAS4 = domibusPropertyProvider.getDomainProperty(DYNAMIC_DISCOVERY_TRANSPORTPROFILEAS4, TransportProfile.AS4.getIdentifier());
            LOG.debug("Getting the Endpoint from ServiceMetadata with transportprofile [{}]", transportProfileAS4);
            final Endpoint endpoint = sm.getEndpoint(processIdentifier, TransportProfile.of(transportProfileAS4));

            if (endpoint == null || endpoint.getAddress() == null) {
                throw new ConfigurationException("Could not fetch metadata from SMP for documentId " + documentId + " processId " + processId);
            }
            return new EndpointInfo(endpoint.getAddress().toString(), endpoint.getCertificate());
        } catch (final PeppolParsingException | PeppolLoadingException | PeppolSecurityException | LookupException | EndpointNotFoundException | IllegalStateException e) {
            throw new ConfigurationException("Could not fetch metadata from SMP for documentId " + documentId + " processId " + processId, e);
        }
    }

    protected DocumentTypeIdentifier getDocumentTypeIdentifier(String documentId) throws PeppolParsingException {
        DocumentTypeIdentifier result = null;
        if(StringUtils.contains(documentId, DocumentTypeIdentifier.DEFAULT_SCHEME.getIdentifier())) {
            LOG.debug("Getting DocumentTypeIdentifier by parsing the document Id [{}]", documentId);
            result = DocumentTypeIdentifier.parse(documentId);
        } else {
            LOG.debug("Getting DocumentTypeIdentifier for the document Id [{}]", documentId);
            result = DocumentTypeIdentifier.of(documentId);
        }
        return result;
    }

    protected ProcessIdentifier getProcessIdentifier(String processId) throws PeppolParsingException {
        ProcessIdentifier result = null;
        if(StringUtils.contains(processId, SCHEME_DELIMITER)) {
            LOG.debug("Getting ProcessIdentifier by parsing the process Id [{}]", processId);
            result = ProcessIdentifier.parse(processId);
        } else {
            LOG.debug("Getting ProcessIdentifier for process Id [{}]", processId);
            result = ProcessIdentifier.of(processId);
        }
        return result;
    }

    @Override
    public String getPartyIdType() {
        return domibusPropertyProvider.getDomainProperty(DYNAMIC_DISCOVERY_PARTYID_TYPE, PARTYID_TYPE);
    }

    @Override
    public String getResponderRole(){
        return domibusPropertyProvider.getDomainProperty(DYNAMIC_DISCOVERY_PARTYID_RESPONDER_ROLE, RESPONDER_ROLE);
    }

}