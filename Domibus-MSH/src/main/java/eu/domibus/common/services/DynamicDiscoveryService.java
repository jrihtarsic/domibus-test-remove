package eu.domibus.common.services;

import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.util.EndpointInfo;

/**
 * @author Ioana Dragusanu (idragusa)
 * @since 3.2.5
 */
public interface DynamicDiscoveryService {
    String SMLZONE_KEY = "domibus.smlzone";
    String DYNAMIC_DISCOVERY_TRANSPORTPROFILEAS4 = "domibus.dynamicdiscovery.transportprofileas4";
    String DYNAMIC_DISCOVERY_MODE = "domibus.dynamicdiscovery.peppolclient.mode";
    String DYNAMIC_DISCOVERY_CERT_REGEX = "domibus.dynamicdiscovery.oasisclient.regexCertificateSubjectValidation";
    String DYNAMIC_DISCOVERY_PARTYID_RESPONDER_ROLE = "domibus.dynamicdiscovery.partyid.responder.role";
    String DYNAMIC_DISCOVERY_PARTYID_TYPE = "domibus.dynamicdiscovery.partyid.type";
    String USE_DYNAMIC_DISCOVERY = "domibus.dynamicdiscovery.useDynamicDiscovery";

    EndpointInfo lookupInformation(final String domain,
                                   final String participantId,
                                   final String participantIdScheme,
                                   final String documentId,
                                   final String processId,
                                   final String processIdScheme) throws EbMS3Exception;

    String getPartyIdType();
    String getResponderRole();
}