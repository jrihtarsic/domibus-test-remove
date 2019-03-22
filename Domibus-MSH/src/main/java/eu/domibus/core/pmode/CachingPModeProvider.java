
package eu.domibus.core.pmode;

import com.google.common.collect.Lists;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.*;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.context.MessageExchangeConfiguration;
import eu.domibus.ebms3.common.model.AgreementRef;
import eu.domibus.ebms3.common.model.MessageExchangePattern;
import eu.domibus.ebms3.common.model.PartyId;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.XmlProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.*;


/**
 * @author Christian Koch, Stefan Mueller
 */
public class CachingPModeProvider extends PModeProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(CachingPModeProvider.class);

    //Don't access directly, use getter instead
    private Configuration configuration;

    @Autowired
    private ProcessPartyExtractorProvider processPartyExtractorProvider;
    //pull processes cache.
    private Map<Party, List<Process>> pullProcessesByInitiatorCache = new HashMap<>();

    private Map<String, List<Process>> pullProcessByMpcCache = new HashMap<>();

    protected Domain domain;

    private Object configurationLock = new Object();

    public CachingPModeProvider(Domain domain) {
        this.domain = domain;
    }

    protected Configuration getConfiguration() {
        if (this.configuration == null) {
            synchronized (configurationLock) {
                if (this.configuration == null) {
                    this.init();
                }
            }
        }
        return this.configuration;
    }

    @Override
    public Party getGatewayParty() {
        return getConfiguration().getParty();
    }

    @Override
    protected void init() {
        if (!this.configurationDAO.configurationExists()) {
            throw new IllegalStateException("No processing modes found. To exchange messages, upload configuration file through the web gui.");
        }
        LOG.debug("Initialising the configuration");
        this.configuration = this.configurationDAO.readEager();
        initPullProcessesCache();
    }

    private void initPullProcessesCache() {
        final Set<Mpc> mpcs = getConfiguration().getMpcs();
        for (Mpc mpc : mpcs) {
            final String qualifiedName = mpc.getQualifiedName();
            final List<Process> pullProcessByMpc = processDao.findPullProcessByMpc(qualifiedName);
            pullProcessByMpcCache.put(qualifiedName, pullProcessByMpc);
        }
        final List<Process> processes = getConfiguration().getBusinessProcesses().getProcesses();
        Set<Party> initiators = new HashSet<>();
        for (Process process : processes) {
            initiators.addAll(process.getInitiatorParties());
        }
        for (Party initiator : initiators) {
            final List<Process> pullProcessesByInitiator = processDao.findPullProcessesByInitiator(initiator);
            pullProcessesByInitiatorCache.put(initiator, pullProcessesByInitiator);
        }
    }

    protected List<Process> keepPullProcessesOnly(List<Process> processes) {
        List<Process> pullProcesses = new ArrayList<>();
        for(Process process : processes) {
            if(MessageExchangePattern.ONE_WAY_PULL.getUri().equals(process.getMepBinding().getValue()))
                pullProcesses.add(process);
        }
        return pullProcesses;
    }

    @Override
    //FIXME: only works for the first leg, as sender=initiator
    protected String findLegName(final String agreementName, final String senderParty, final String receiverParty, final String service, final String action, final String mpc, final Boolean pullOnly) throws EbMS3Exception {
        final List<LegConfiguration> candidates = new ArrayList<>();
        List<Process> processes = this.getConfiguration().getBusinessProcesses().getProcesses();
        if (pullOnly) {
            processes = keepPullProcessesOnly(processes);
        }

        for (final Process process : processes) {
            final ProcessTypePartyExtractor processTypePartyExtractor = processPartyExtractorProvider.getProcessTypePartyExtractor(process.getMepBinding().getValue(), senderParty, receiverParty);
            for (final Party party : process.getInitiatorParties()) {
                if (StringUtils.equalsIgnoreCase(party.getName(), processTypePartyExtractor.getSenderParty())) {
                    for (final Party responder : process.getResponderParties()) {
                        if (StringUtils.equalsIgnoreCase(responder.getName(), processTypePartyExtractor.getReceiverParty())) {
                            if (process.getAgreement() != null && StringUtils.equalsIgnoreCase(process.getAgreement().getName(), agreementName)
                                    || (StringUtils.equalsIgnoreCase(agreementName, OPTIONAL_AND_EMPTY) && process.getAgreement() == null)
                                    // Please notice that this is only for backward compatibility and will be removed ASAP!
                                    || (StringUtils.equalsIgnoreCase(agreementName, OPTIONAL_AND_EMPTY) && process.getAgreement() != null && StringUtils.isEmpty(process.getAgreement().getValue()))
                                    ) {
                                /**
                                 * The Process is a candidate because either has an Agreement and its name matches the Agreement name found previously
                                 * or it has no Agreement configured and the Agreement name was not indicated in the submitted message.
                                 **/
                                candidates.addAll(process.getLegs());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            LOG.businessError(DomibusMessageCode.BUS_LEG_NAME_NOT_FOUND, agreementName, senderParty, receiverParty, service, action);
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No Candidates for Legs found", null, null);
        }
        for (final LegConfiguration candidate : candidates) {
            if (StringUtils.equalsIgnoreCase(candidate.getService().getName(), service)
                    && StringUtils.equalsIgnoreCase(candidate.getAction().getName(), action)
                    && (mpc == null || StringUtils.equalsIgnoreCase(candidate.getDefaultMpc().getName(), mpc))) {
                return candidate.getName();
            }
        }
        LOG.businessError(DomibusMessageCode.BUS_LEG_NAME_NOT_FOUND, agreementName, senderParty, receiverParty, service, action);
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching leg found", null, null);
    }

    @Override
    protected String findActionName(final String action) throws EbMS3Exception {
        for (final Action action1 : this.getConfiguration().getBusinessProcesses().getActions()) {
            if (StringUtils.equalsIgnoreCase(action1.getValue(), action)) {
                return action1.getName();
            }
        }
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching action found", null, null);
    }


    @Override
    protected String findMpcName(final String mpc) throws EbMS3Exception {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getQualifiedName(), mpc)) {
                return mpc1.getName();
            }
        }
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching mpc found", null, null);
    }

    @Override
    protected String findServiceName(final eu.domibus.ebms3.common.model.Service service) throws EbMS3Exception {
        if (service == null) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "Service is not found in the message", null, null);
        }

        for (final Service service1 : this.getConfiguration().getBusinessProcesses().getServices()) {
            if ((StringUtils.equalsIgnoreCase(service1.getServiceType(), service.getType()) || (!StringUtils.isNotEmpty(service1.getServiceType()) && !StringUtils.isNotEmpty(service.getType()))))
                if (StringUtils.equalsIgnoreCase(service1.getValue(), service.getValue())) {
                    return service1.getName();
                }
        }
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching service found", null, null);
    }

    @Override
    //@Transactional(propagation = Propagation.SUPPORTS, noRollbackFor = IllegalStateException.class)
    protected String findPartyName(final Collection<PartyId> partyId) throws EbMS3Exception {
        String partyIdType = "";
        for (final Party party : this.getConfiguration().getBusinessProcesses().getParties()) {
            for (final PartyId id : partyId) {
                for (final Identifier identifier : party.getIdentifiers()) {
                    if (id.getType() != null) {
                        partyIdType = id.getType();
                        try {
                            URI.create(partyIdType);
                        } catch (final IllegalArgumentException e) {
                            final EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0003, "no matching party found", null, e);
                            ex.setErrorDetail("PartyId " + id.getValue() + " is not a valid URI [CORE]");
                            throw ex;
                        }
                    }
                    String identifierPartyIdType = "";
                    if (identifier.getPartyIdType() != null) {
                        identifierPartyIdType = identifier.getPartyIdType().getValue();
                    }

                    if (StringUtils.equalsIgnoreCase(partyIdType, identifierPartyIdType) && StringUtils.equalsIgnoreCase(id.getValue(), identifier.getPartyId())) {
                        return party.getName();
                    }
                }
            }
        }
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0003, "No matching party found", null, null);
    }

    @Override
    protected String findAgreement(final AgreementRef agreementRef) throws EbMS3Exception {
        if (agreementRef == null || agreementRef.getValue() == null || agreementRef.getValue().isEmpty()) {
            return OPTIONAL_AND_EMPTY; // AgreementRef is optional
        }

        for (final Agreement agreement : this.getConfiguration().getBusinessProcesses().getAgreements()) {
            if ((StringUtils.isEmpty(agreementRef.getType()) || StringUtils.equalsIgnoreCase(agreement.getType(), agreementRef.getType()))
                    && StringUtils.equalsIgnoreCase(agreementRef.getValue(), agreement.getValue())) {
                return agreement.getName();
            }
        }
        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "No matching agreement found", null, null);
    }

    @Override
    public Party getPartyByIdentifier(String partyIdentifier) {
        for (final Party party : this.getConfiguration().getBusinessProcesses().getParties()) {
            final Set<Identifier> identifiers = party.getIdentifiers();
            for (Identifier identifier : identifiers) {
                if (StringUtils.equalsIgnoreCase(identifier.getPartyId(), partyIdentifier)) {
                    return party;
                }
            }
        }
        return null;
    }

    @Override
    public Party getSenderParty(final String pModeKey) {
        final String partyKey = this.getSenderPartyNameFromPModeKey(pModeKey);
        for (final Party party : this.getConfiguration().getBusinessProcesses().getParties()) {
            if (StringUtils.equalsIgnoreCase(party.getName(), partyKey)) {
                return party;
            }
        }
        throw new ConfigurationException("no matching sender party found with name: " + partyKey);
    }

    @Override
    public Party getReceiverParty(final String pModeKey) {
        final String partyKey = this.getReceiverPartyNameFromPModeKey(pModeKey);
        for (final Party party : this.getConfiguration().getBusinessProcesses().getParties()) {
            if (StringUtils.equalsIgnoreCase(party.getName(), partyKey)) {
                return party;
            }
        }
        throw new ConfigurationException("no matching receiver party found with name: " + partyKey);
    }

    @Override
    public Service getService(final String pModeKey) {
        final String serviceKey = this.getServiceNameFromPModeKey(pModeKey);
        for (final Service service : this.getConfiguration().getBusinessProcesses().getServices()) {
            if (StringUtils.equalsIgnoreCase(service.getName(), serviceKey)) {
                return service;
            }
        }
        throw new ConfigurationException("no matching service found with name: " + serviceKey);
    }

    @Override
    public Action getAction(final String pModeKey) {
        final String actionKey = this.getActionNameFromPModeKey(pModeKey);
        for (final Action action : this.getConfiguration().getBusinessProcesses().getActions()) {
            if (StringUtils.equalsIgnoreCase(action.getName(), actionKey)) {
                return action;
            }
        }
        throw new ConfigurationException("no matching action found with name: " + actionKey);
    }

    @Override
    public Agreement getAgreement(final String pModeKey) {
        final String agreementKey = this.getAgreementRefNameFromPModeKey(pModeKey);
        for (final Agreement agreement : this.getConfiguration().getBusinessProcesses().getAgreements()) {
            if (StringUtils.equalsIgnoreCase(agreement.getName(), agreementKey)) {
                return agreement;
            }
        }
        throw new ConfigurationException("no matching agreement found with name: " + agreementKey);
    }

    @Override
    public LegConfiguration getLegConfiguration(final String pModeKey) {
        final String legKey = this.getLegConfigurationNameFromPModeKey(pModeKey);
        for (final LegConfiguration legConfiguration : this.getConfiguration().getBusinessProcesses().getLegConfigurations()) {
            if (StringUtils.equalsIgnoreCase(legConfiguration.getName(), legKey)) {
                return legConfiguration;
            }
        }
        throw new ConfigurationException("no matching legConfiguration found with name: " + legKey);
    }

    @Override
    public boolean isMpcExistant(final String mpc) {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getName(), mpc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getRetentionDownloadedByMpcName(final String mpcName) {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getName(), mpcName)) {
                return mpc1.getRetentionDownloaded();
            }
        }

        LOG.error("No MPC with name: [{}] found. Assuming message retention of 0 for downloaded messages.", mpcName);

        return 0;
    }

    @Override
    public int getRetentionDownloadedByMpcURI(final String mpcURI) {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getQualifiedName(), mpcURI)) {
                return mpc1.getRetentionDownloaded();
            }
        }

        LOG.error("No MPC with name: [{}] found. Assuming message retention of 0 for downloaded messages.", mpcURI);

        return 0;
    }

    @Override
    public int getRetentionUndownloadedByMpcName(final String mpcName) {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getName(), mpcName)) {
                return mpc1.getRetentionUndownloaded();
            }
        }

        LOG.error("No MPC with name: [{}] found. Assuming message retention of -1 for undownloaded messages.", mpcName);

        return -1;
    }

    @Override
    public int getRetentionUndownloadedByMpcURI(final String mpcURI) {
        for (final Mpc mpc1 : this.getConfiguration().getMpcs()) {
            if (StringUtils.equalsIgnoreCase(mpc1.getQualifiedName(), mpcURI)) {
                return mpc1.getRetentionUndownloaded();
            }
        }

        LOG.error("No MPC with name: [{}] found. Assuming message retention of -1 for undownloaded messages.", mpcURI);

        return -1;
    }

    @Override
    public List<String> getMpcList() {
        final List<String> result = new ArrayList<>();
        for (final Mpc mpc : this.getConfiguration().getMpcs()) {
            result.add(mpc.getName());
        }
        return result;
    }

    @Override
    public List<String> getMpcURIList() {
        final List<String> result = new ArrayList<>();
        for (final Mpc mpc : this.getConfiguration().getMpcs()) {
            result.add(mpc.getQualifiedName());
        }
        return result;
    }

    @Override
    public Role getBusinessProcessRole(String roleValue) {
        for (Role role : this.getConfiguration().getBusinessProcesses().getRoles()) {
            if (StringUtils.equalsIgnoreCase(role.getValue(), roleValue)) {
                return role;
            }
        }
        LOG.businessError(DomibusMessageCode.BUS_PARTY_ROLE_NOT_FOUND, roleValue);
        return null;
    }

    @Override
    public void refresh() {
        this.configuration = null;
        this.getConfiguration(); //reloads the config
    }

    @Override
    public boolean isConfigurationLoaded() {
        if (this.configuration != null) return true;
        return configurationDAO.configurationExists();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<String> updatePModes(final byte[] bytes, String description) throws XmlProcessingException {
        List<String> messages = super.updatePModes(bytes, description);
        this.configuration = null;
        this.pullProcessByMpcCache.clear();
        this.pullProcessesByInitiatorCache.clear();
        return messages;
    }

    @Override
    public List<Process> findPullProcessesByMessageContext(final MessageExchangeConfiguration messageExchangeConfiguration) {
        return processDao.findPullProcessesByMessageContext(messageExchangeConfiguration);
    }

    @Override
    public List<Process> findPullProcessesByInitiator(final Party party) {
        final List<Process> processes = pullProcessesByInitiatorCache.get(party);
        if (processes == null) {
            return Lists.newArrayList();
        }
        return processes;
    }

    @Override
    public List<Process> findPullProcessByMpc(final String mpc) {
        List<Process> processes = pullProcessByMpcCache.get(mpc);
        if (processes == null) {
            return Lists.newArrayList();
        }
        return processes;
    }

    @Override
    public List<Process> findAllProcesses() {
        try {
            return Lists.newArrayList(getConfiguration().getBusinessProcesses().getProcesses());
        } catch (IllegalArgumentException e) {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<Party> findAllParties() {
        try {
            return Lists.newArrayList(getConfiguration().getBusinessProcesses().getParties());
        } catch (IllegalArgumentException e) {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<String> findPartyIdByServiceAndAction(String service, String action) {
        List result = new ArrayList<String>();
        for (Process process : this.getConfiguration().getBusinessProcesses().getProcesses()) {
            for (LegConfiguration legConfiguration : process.getLegs()) {
                result.addAll(handleLegConfiguration(legConfiguration, process, service, action));
            }
        }
        return result;
    }

    private List<String> handleLegConfiguration(LegConfiguration legConfiguration, Process process, String service, String action) {
        List result = new ArrayList<String>();
        if (StringUtils.equalsIgnoreCase(legConfiguration.getService().getValue(), service) && StringUtils.equalsIgnoreCase(legConfiguration.getAction().getValue(), action)) {
            handleProcessParties(process, result);
        }
        return result;
    }

    private void handleProcessParties(Process process, List result) {
        for (Party party : process.getResponderParties()) {
            for (Identifier identifier : party.getIdentifiers()) {
                result.add(identifier.getPartyId());
            }
        }
    }

    @Override
    public String getPartyIdType(String partyIdentifier) {
        for (Party party : getConfiguration().getBusinessProcesses().getParties()) {
            String partyIdTypeHandleParty = getPartyIdTypeHandleParty(party, partyIdentifier);
            if (partyIdTypeHandleParty != null) {
                return partyIdTypeHandleParty;
            }
        }
        return null;
    }

    private String getPartyIdTypeHandleParty(Party party, String partyIdentifier) {
        for (Identifier identifier : party.getIdentifiers()) {
            if (StringUtils.equalsIgnoreCase(identifier.getPartyId(), partyIdentifier)) {
                return identifier.getPartyIdType().getValue();
            }
        }
        return null;
    }

    @Override
    public String getServiceType(String serviceValue) {
        for (Service service : getConfiguration().getBusinessProcesses().getServices()) {
            if (StringUtils.equalsIgnoreCase(service.getValue(), serviceValue)) {
                return service.getServiceType();
            }
        }
        return null;
    }

    protected List<Process> getProcessFromService(String serviceValue) {
        List<Process> result = new ArrayList<>();
        for (Process process : getConfiguration().getBusinessProcesses().getProcesses()) {
            for (LegConfiguration legConfiguration : process.getLegs()) {
                if (StringUtils.equalsIgnoreCase(legConfiguration.getService().getValue(), serviceValue)) {
                    result.add(process);
                }
            }
        }
        return result;
    }

    @Override
    public String getRole(String roleType, String serviceValue) {
        for (Process found : getProcessFromService(serviceValue)) {
            String roleHandleProcess = getRoleHandleProcess(found, roleType);
            if (roleHandleProcess != null) {
                return roleHandleProcess;
            }
        }
        return null;
    }

    @Nullable
    private String getRoleHandleProcess(Process found, String roleType) {
        for (Process process : getConfiguration().getBusinessProcesses().getProcesses()) {
            if (StringUtils.equalsIgnoreCase(process.getName(), found.getName())) {
                if (roleType.equalsIgnoreCase("initiator")) {
                    return process.getInitiatorRole().getValue();
                }
                if (roleType.equalsIgnoreCase("responder")) {
                    return process.getResponderRole().getValue();
                }
            }
        }
        return null;
    }

    @Override
    public String getAgreementRef(String serviceValue) {
        for (Process found : getProcessFromService(serviceValue)) {
            String agreementRefHandleProcess = getAgreementRefHandleProcess(found);
            if (agreementRefHandleProcess != null) {
                return agreementRefHandleProcess;
            }
        }
        return null;
    }

    @Nullable
    private String getAgreementRefHandleProcess(Process found) {
        for (Process process : getConfiguration().getBusinessProcesses().getProcesses()) {
            if (StringUtils.equalsIgnoreCase(process.getName(), found.getName())) {
                Agreement agreement = process.getAgreement();
                if (agreement != null) {
                    return agreement.getValue();
                }
            }
        }
        return null;
    }
}
