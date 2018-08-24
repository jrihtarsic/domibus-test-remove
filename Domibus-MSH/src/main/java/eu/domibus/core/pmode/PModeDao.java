package eu.domibus.core.pmode;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.dao.PartyDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.*;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.context.MessageExchangeConfiguration;
import eu.domibus.ebms3.common.model.AgreementRef;
import eu.domibus.ebms3.common.model.PartyId;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Transactional
public class PModeDao extends PModeProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeDao.class);
    private static final String STR_ACTION = "ACTION";
    private static final String STR_SERVICE = "SERVICE";
    private static final String STR_NO_MATCHING_LEG_FOUND = "No matching leg found";

    @Autowired
    private PartyDao partyDao;

    @Override
    public Party getGatewayParty() {
        //TODO check if it can be optimized
        return configurationDAO.read().getParty();
    }

    @Override
    public Party getSenderParty(final String pModeKey) {

        String senderPartyName = this.getSenderPartyNameFromPModeKey(pModeKey);

        final TypedQuery<Party> query = this.entityManager.createNamedQuery("Party.findByName", Party.class);
        query.setParameter("NAME", senderPartyName);
        try {
            return query.getSingleResult();
        } catch (NoResultException pEx) {
            LOG.businessError(DomibusMessageCode.BUS_PARTY_NAME_NOT_FOUND, senderPartyName);
            return null;
        }
    }

    @Override
    public Party getReceiverParty(final String pModeKey) {

        String senderPartyName = this.getReceiverPartyNameFromPModeKey(pModeKey);

        final TypedQuery<Party> query = this.entityManager.createNamedQuery("Party.findByName", Party.class);
        query.setParameter("NAME", senderPartyName);
        try {
            return query.getSingleResult();
        } catch (NoResultException pEx) {
            LOG.businessError(DomibusMessageCode.BUS_PARTY_NAME_NOT_FOUND, senderPartyName);
            return null;
        }
    }

    @Override
    public Service getService(final String pModeKey) {
        final TypedQuery<Service> query = this.entityManager.createNamedQuery("Service.findByName", Service.class);
        query.setParameter("NAME", this.getServiceNameFromPModeKey(pModeKey)); //FIXME enable multiple ServiceTypes with the same name
        return query.getSingleResult();
    }

    @Override
    public Action getAction(final String pModeKey) {
        final TypedQuery<Action> query = this.entityManager.createNamedQuery("Action.findByName", Action.class);
        query.setParameter("NAME", this.getActionNameFromPModeKey(pModeKey));
        return query.getSingleResult();
    }

    @Override
    public Agreement getAgreement(final String pModeKey) {
        final TypedQuery<Agreement> query = this.entityManager.createNamedQuery("Agreement.findByName", Agreement.class);
        query.setParameter("NAME", this.getAgreementRefNameFromPModeKey(pModeKey));
        return query.getSingleResult();
    }

    @Override
    public LegConfiguration getLegConfiguration(final String pModeKey) {
        final TypedQuery<LegConfiguration> query = this.entityManager.createNamedQuery("LegConfiguration.findByName", LegConfiguration.class);
        query.setParameter("NAME", this.getLegConfigurationNameFromPModeKey(pModeKey));
        return query.getSingleResult();
    }


    @Override
    public void init() {
        //nothing to init here
    }

    protected String findLegName(final String agreementName, final String senderParty, final String receiverParty, final String service, final String action) throws EbMS3Exception {
        try {
            //this is the normal call for a push.
            return findLegNameMepBindingAgnostic(agreementName, senderParty, receiverParty, service, action);
        } catch (EbMS3Exception e) {
            //Here we invert the parties to find leg configured for a pull.
            try {
                String legNameInPullProcess = findLegNameMepBindingAgnostic(agreementName, receiverParty, senderParty, service, action);
                //then we verify that the leg is indeed in a pull process.
                final List<Process> resultList = processDao.findPullProcessByLegName(legNameInPullProcess);
                //if not pull process found then this is a miss configuration.
                if (resultList.isEmpty()) {
                    throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, STR_NO_MATCHING_LEG_FOUND, null, null);
                }
                return legNameInPullProcess;
            } catch (EbMS3Exception e1) {
                LOG.businessError(DomibusMessageCode.BUS_LEG_NAME_NOT_FOUND, e, agreementName, senderParty, receiverParty, service, action);
                throw e1;
            }
        }

    }

    public String findLegNameMepBindingAgnostic(String agreementName, String senderParty, String receiverParty, String service, String action) throws EbMS3Exception {
        LOG.debug("Finding leg name using agreement [{}], senderParty [{}], receiverParty [{}], service [{}] and action [{}]",
                agreementName, senderParty, receiverParty, service, action);
        String namedQuery;
        if (agreementName.equalsIgnoreCase(OPTIONAL_AND_EMPTY)) {
            namedQuery = "LegConfiguration.findForPartiesAndAgreementsOAE";
        } else {
            namedQuery = "LegConfiguration.findForPartiesAndAgreements";
        }
        LOG.debug("Using named query [{}]", namedQuery);

        Query candidatesQuery = this.entityManager.createNamedQuery(namedQuery);
        if (!agreementName.equalsIgnoreCase(OPTIONAL_AND_EMPTY)) {
            LOG.debug("Setting agreement [{}]", OPTIONAL_AND_EMPTY);
            candidatesQuery.setParameter("AGREEMENT", agreementName);
        }
        candidatesQuery.setParameter("SENDER_PARTY", senderParty);
        candidatesQuery.setParameter("RECEIVER_PARTY", receiverParty);

        List<LegConfiguration> candidates = candidatesQuery.getResultList();
        if (candidates == null || candidates.isEmpty()) {
            // To be removed when the backward compatibility will be finally broken!
            namedQuery = "LegConfiguration.findForPartiesAndAgreementEmpty";
            LOG.debug("No candidates found, using namedQuery to find candidates [{}]", namedQuery);
            candidatesQuery = this.entityManager.createNamedQuery(namedQuery);
            candidatesQuery.setParameter("SENDER_PARTY", senderParty);
            candidatesQuery.setParameter("RECEIVER_PARTY", receiverParty);
            candidates = candidatesQuery.getResultList();
            if (candidates == null || candidates.isEmpty()) {
                LOG.businessError(DomibusMessageCode.BUS_LEG_NAME_NOT_FOUND, agreementName, senderParty, receiverParty, service, action);
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, STR_NO_MATCHING_LEG_FOUND, null, null);
            }
        }
        final TypedQuery<String> query = this.entityManager.createNamedQuery("LegConfiguration.findForPMode", String.class);
        query.setParameter(STR_SERVICE, service);
        query.setParameter(STR_ACTION, action);
        final Collection<String> candidateIds = new HashSet<>();
        for (final LegConfiguration candidate : candidates) {
            candidateIds.add(candidate.getName());
        }
        query.setParameter("CANDIDATES", candidateIds);
        try {
            return query.getSingleResult();
        } catch (final NoResultException e) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, STR_NO_MATCHING_LEG_FOUND, null, null);
        }
    }

    protected String findAgreement(final AgreementRef agreementRef) throws EbMS3Exception {
        if (agreementRef == null || agreementRef.getValue() == null || agreementRef.getValue().isEmpty()) {
            return OPTIONAL_AND_EMPTY;
        }
        final String value = agreementRef.getValue();
        final String type = agreementRef.getType();
        final TypedQuery<String> query = this.entityManager.createNamedQuery("Agreement.findByValueAndType", String.class);
        query.setParameter("VALUE", value);
        query.setParameter("TYPE", (type == null) ? "" : type);
        try {
            return query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_AGREEMENT_NOT_FOUND, e, agreementRef);
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching agreement found", null, null);
        }
    }

    protected String findActionName(final String action) throws EbMS3Exception {
        if (action == null || action.isEmpty()) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "Action parameter must not be null or empty", null, null);
        }

        final TypedQuery<String> query = this.entityManager.createNamedQuery("Action.findByAction", String.class);
        query.setParameter(STR_ACTION, action);
        try {
            return query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_ACTION_NOT_FOUND, e, action);
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching action found", null, null);
        }
    }

    protected String findServiceName(final eu.domibus.ebms3.common.model.Service service) throws EbMS3Exception {
        final String type = service.getType();
        final String value = service.getValue();
        final TypedQuery<String> query;
        if (type == null || type.isEmpty()) {
            try {
                URI.create(value); //if not an URI an IllegalArgumentException will be thrown
                query = entityManager.createNamedQuery("Service.findWithoutType", String.class);
                query.setParameter(STR_SERVICE, value);
            } catch (final IllegalArgumentException e) {
                LOG.businessError(DomibusMessageCode.BUS_MESSAGE_SERVICE_INVALID_URI, value);
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0003, "Service " + value + " is not a valid URI [CORE]", null, e);
            }
        } else {
            query = this.entityManager.createNamedQuery("Service.findByServiceAndType", String.class);
            query.setParameter(STR_SERVICE, value);
            query.setParameter("TYPE", type);
        }
        try {
            return query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_SERVICE_NOT_FOUND, e);
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching service found", null, null);
        }
    }

    protected String findPartyName(final Collection<PartyId> partyIds) throws EbMS3Exception {
        Identifier identifier;
        for (final PartyId partyId : partyIds) {
            LOG.debug("Trying to find party [{}]", partyId);
            try {
                String type = partyId.getType();
                if (type == null || type.isEmpty()) { //PartyId must be an URI
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        URI.create(partyId.getValue()); //if not an URI an IllegalArgumentException will be thrown
                        type = "";
                    } catch (final IllegalArgumentException e) {
                        LOG.businessError(DomibusMessageCode.BUS_PARTY_ID_INVALID_URI, partyId.getValue());
                        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0003, "PartyId " + partyId.getValue() + " is not a valid URI [CORE]", null, e);
                    }
                }
                final TypedQuery<Identifier> identifierQuery = this.entityManager.createNamedQuery("Identifier.findByTypeAndPartyId", Identifier.class);
                identifierQuery.setParameter("PARTY_ID", partyId.getValue());
                identifierQuery.setParameter("PARTY_ID_TYPE", type);
                identifier = identifierQuery.getSingleResult();
                LOG.debug("Found identifier [{}]", identifier);
                final TypedQuery<String> query = this.entityManager.createNamedQuery("Party.findPartyByIdentifier", String.class);
                query.setParameter("PARTY_IDENTIFIER", identifier);

                return query.getSingleResult();
            } catch (final NoResultException e) {
                LOG.debug("", e); // Its ok to not know all identifiers, we just have to know one
            }
        }
        LOG.businessError(DomibusMessageCode.BUS_PARTY_ID_NOT_FOUND, partyIds);
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0003, "No matching party found", null, null);
    }

    @Override
    public boolean isMpcExistant(final String mpc) {
        final TypedQuery<Integer> query = this.entityManager.createNamedQuery("Mpc.countForQualifiedName", Integer.class);
        return query.getSingleResult() > 0;
    }

    @Override
    public int getRetentionDownloadedByMpcName(final String mpcName) {

        final TypedQuery<Mpc> query = entityManager.createNamedQuery("Mpc.findByName", Mpc.class);
        query.setParameter("NAME", mpcName);

        final Mpc result = query.getSingleResult();

        if (result == null) {
            LOG.error("No MPC with name: [{}] found. Assuming message retention of 0 for downloaded messages.", mpcName);
            return 0;
        }

        return result.getRetentionDownloaded();
    }

    @Override
    public int getRetentionDownloadedByMpcURI(final String mpcURI) {

        final TypedQuery<Mpc> query = entityManager.createNamedQuery("Mpc.findByQualifiedName", Mpc.class);
        query.setParameter("QUALIFIED_NAME", mpcURI);

        final Mpc result = query.getSingleResult();

        if (result == null) {
            LOG.error("No MPC with name: [{}] found. Assuming message retention of 0 for downloaded messages.", mpcURI);
            return 0;
        }

        return result.getRetentionDownloaded();
    }

    @Override
    public int getRetentionUndownloadedByMpcName(final String mpcName) {

        final TypedQuery<Mpc> query = this.entityManager.createNamedQuery("Mpc.findByName", Mpc.class);
        query.setParameter("NAME", mpcName);

        final Mpc result = query.getSingleResult();

        if (result == null) {
            LOG.error("No MPC with name: [{}] found. Assuming message retention of -1 for undownloaded messages.", mpcName);
            return 0;
        }

        return result.getRetentionUndownloaded();
    }

    @Override
    public int getRetentionUndownloadedByMpcURI(final String mpcURI) {

        final TypedQuery<Mpc> query = entityManager.createNamedQuery("Mpc.findByQualifiedName", Mpc.class);
        query.setParameter("QUALIFIED_NAME", mpcURI);

        final Mpc result = query.getSingleResult();

        if (result == null) {
            LOG.error("No MPC with name: [{}] found. Assuming message retention of -1 for undownloaded messages.", mpcURI);
            return 0;
        }

        return result.getRetentionUndownloaded();
    }

    @Override
    public List<String> getMpcList() {
        final TypedQuery<String> query = entityManager.createNamedQuery("Mpc.getAllNames", String.class);
        return query.getResultList();
    }

    @Override
    public List<String> getMpcURIList() {
        final TypedQuery<String> query = entityManager.createNamedQuery("Mpc.getAllURIs", String.class);
        return query.getResultList();
    }

    @Override
    public void refresh() {
        //as we always query the DB pmodes never are stale, thus no refresh needed
    }

    @Override
    public boolean isConfigurationLoaded() {
        return configurationDAO.configurationExists();
    }

    @Override
    public Role getBusinessProcessRole(String roleValue) {
        final TypedQuery<Role> query = entityManager.createNamedQuery("Role.findByValue", Role.class);
        query.setParameter("VALUE", roleValue);

        try {
            return query.getSingleResult();
        } catch (NoResultException pEx) {
            LOG.businessError(DomibusMessageCode.BUS_PARTY_ROLE_NOT_FOUND, roleValue);
            return null;
        }
    }

    @Override
    public List<Process> findPullProcessesByMessageContext(final MessageExchangeConfiguration messageExchangeConfiguration) {
        return processDao.findPullProcessesByMessageContext(messageExchangeConfiguration);
    }

    @Override
    public List<Process> findPullProcessesByInitiator(final Party party) {
        return processDao.findPullProcessesByInitiator(party);
    }

    @Override
    public List<Process> findPullProcessByMpc(final String mpc) {
        return processDao.findPullProcessByMpc(mpc);
    }

    @Override
    public List<Process> findAllProcesses() {
        return processDao.findAllProcesses();
    }

    @Override
    public List<Party> findAllParties() {
        return partyDao.getParties();
    }


    @Override
    public List<String> findPartyIdByServiceAndAction(final String service, final String action) {
        List<String> result = new ArrayList<>();
        LegConfiguration legConfiguration;
        // get the leg which contains the service and action
        final TypedQuery<LegConfiguration> query = this.entityManager.createNamedQuery("LegConfiguration.findForTestService", LegConfiguration.class);
        query.setParameter(STR_SERVICE, service);
        query.setParameter(STR_ACTION, action);
        try {
            legConfiguration = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No matching leg was found", e);
            return result;
        }

        final List<Process> processByLegName = processDao.findProcessByLegName(legConfiguration.getName());

        for (Process process : processByLegName) {
            for(Party party : process.getResponderParties()) {
                for(Identifier identifier : party.getIdentifiers()) {
                    result.add(identifier.getPartyId());
                }
            }
        }
        return result;
    }

    @Override
    public String getPartyIdType(String partyIdentifier) {
        // Not implemented on purpose
        return null;
    }

    @Override
    public String getServiceType(String serviceValue) {
        // Not implemented on purpose
        return null;
    }

    @Override
    public String getRole(String roleType, String serviceValue) {
        // Not implemented on purpose
        return null;
    }

    @Override
    public String getAgreementRef(String serviceValue) {
        // Not implemented on purpose
        return null;
    }
}
