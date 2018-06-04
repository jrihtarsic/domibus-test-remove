
package eu.domibus.ebms3.common.dao;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.clustering.Command;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.dao.ConfigurationRawDAO;
import eu.domibus.common.dao.ProcessDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.*;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.context.MessageExchangeConfiguration;
import eu.domibus.ebms3.common.model.AgreementRef;
import eu.domibus.ebms3.common.model.Ebms3Constants;
import eu.domibus.ebms3.common.model.PartyId;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.common.validators.ConfigurationValidator;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.XmlProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * @author Christian Koch, Stefan Mueller
 */
public abstract class PModeProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeProvider.class);

    public static final String SCHEMAS_DIR = "schemas/";
    public static final String DOMIBUS_PMODE_XSD = "domibus-pmode.xsd";

    protected static final String OPTIONAL_AND_EMPTY = "OAE";

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected ConfigurationRawDAO configurationRawDAO;

    @PersistenceContext(unitName = "domibusJTA")
    protected EntityManager entityManager;

    @Autowired()
    @Qualifier("jaxbContextConfig")
    private JAXBContext jaxbContext;

    @Qualifier("jmsTemplateCommand")
    @Autowired
    private JmsOperations jmsOperations;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    XMLUtil xmlUtil;

    @Autowired
    List<ConfigurationValidator> configurationValidators;

    @Autowired
    protected ProcessDao processDao;

    public abstract void init();

    public abstract void refresh();

    public abstract boolean isConfigurationLoaded();

    public byte[] getPModeFile(int id) {
        final ConfigurationRaw rawConfiguration = getRawConfiguration(id);
        if (rawConfiguration != null) {
            return rawConfiguration.getXml();
        }
        return new byte[0];
    }

    public ConfigurationRaw getRawConfiguration(int id) {
        return this.configurationRawDAO.getConfigurationRaw(id);
    }

    public void removePMode(int id) {
        LOG.debug("Removing PMode with id: [{}]", id);
        configurationRawDAO.deleteById(id);
    }

    public List<PModeArchiveInfo> getRawConfigurationList() {
        return this.configurationRawDAO.getDetailedConfigurationRaw();
    }

    protected UnmarshallerResult parsePMode(byte[] bytes) throws XmlProcessingException {
        //unmarshall the PMode with whitespaces ignored
        UnmarshallerResult unmarshalledConfigurationWithWhiteSpacesIgnored = unmarshall(bytes, true);

        if (!unmarshalledConfigurationWithWhiteSpacesIgnored.isValid()) {
            String errorMessage = "The PMode file is not XSD compliant(whitespaces are ignored). Please correct the issues: [" + unmarshalledConfigurationWithWhiteSpacesIgnored.getErrorMessage() + "]";
            XmlProcessingException xmlProcessingException = new XmlProcessingException(errorMessage);
            xmlProcessingException.addErrors(unmarshalledConfigurationWithWhiteSpacesIgnored.getErrors());
            throw xmlProcessingException;
        }

        //unmarshall the PMode taking into account the whitespaces
        return  unmarshall(bytes, false);

    }

    public Configuration getPModeConfiguration(byte[] bytes) throws XmlProcessingException {
        final UnmarshallerResult unmarshallerResult = parsePMode(bytes);
        return unmarshallerResult.getResult();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_AP_ADMIN')")
    public List<String> updatePModes(byte[] bytes, String description) throws XmlProcessingException {
        LOG.debug("Updating the PMode");

        List<String> resultMessage = new ArrayList<>();
        final UnmarshallerResult unmarshalledConfiguration = parsePMode(bytes);
        if (!unmarshalledConfiguration.isValid()) {
            resultMessage.add("The PMode file is not XSD compliant. It is recommended to correct the issues:");
            resultMessage.addAll(unmarshalledConfiguration.getErrors());
            LOG.warn(StringUtils.join(resultMessage, " "));
        }

        Configuration configuration = unmarshalledConfiguration.getResult();
        configurationDAO.updateConfiguration(configuration);

        for (ConfigurationValidator validator : configurationValidators) {
            resultMessage.addAll(validator.validate(configuration));
        }

        //save the raw configuration
        final ConfigurationRaw configurationRaw = new ConfigurationRaw();
        configurationRaw.setConfigurationDate(Calendar.getInstance().getTime());
        configurationRaw.setXml(bytes);
        configurationRaw.setDescription(description);
        configurationRawDAO.create(configurationRaw);

        LOG.info("Configuration successfully updated");
        // Sends a message into the topic queue in order to refresh all the singleton instances of the PModeProvider.
        jmsOperations.send(new ReloadPmodeMessageCreator());

        return resultMessage;
    }


    protected UnmarshallerResult unmarshall(byte[] bytes, boolean ignoreWhitespaces) throws XmlProcessingException {
        Configuration configuration = null;
        UnmarshallerResult unmarshallerResult = null;

        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);
        ByteArrayInputStream xmlStream = new ByteArrayInputStream(bytes);

        try {
            unmarshallerResult = xmlUtil.unmarshal(ignoreWhitespaces, jaxbContext, xmlStream, xsdStream);
            configuration = unmarshallerResult.getResult();
        } catch (JAXBException | SAXException | ParserConfigurationException | XMLStreamException e) {
            LOG.error("Error unmarshalling the PMode", e);
            throw new XmlProcessingException("Error unmarshalling the PMode: " + e.getMessage(), e);
        }
        if (configuration == null) {
            throw new XmlProcessingException("Error unmarshalling the PMode: could not process the PMode file");
        }
        return unmarshallerResult;
    }

    public byte[] serializePModeConfiguration(Configuration configuration) throws XmlProcessingException {

        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);

        byte[] serializedPMode;
        try {
            serializedPMode = xmlUtil.marshal(jaxbContext, configuration, xsdStream);
        } catch (JAXBException | SAXException | ParserConfigurationException | XMLStreamException e) {
            LOG.error("Error marshalling the PMode", e);
            throw new XmlProcessingException("Error marshalling the PMode: " + e.getMessage(), e);
        }
        return serializedPMode;
    }

    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = IllegalStateException.class)
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public MessageExchangeConfiguration findUserMessageExchangeContext(final UserMessage userMessage, final MSHRole mshRole) throws EbMS3Exception {

        final String agreementName;
        final String senderParty;
        final String receiverParty;
        final String service;
        final String action;
        final String leg;

        final String messageId = userMessage.getMessageInfo().getMessageId();
        //add messageId to MDC map
        if (StringUtils.isNotBlank(messageId)) {
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);
        }


        try {
            agreementName = findAgreement(userMessage.getCollaborationInfo().getAgreementRef());
            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_AGREEMENT_FOUND, agreementName, userMessage.getCollaborationInfo().getAgreementRef());
            senderParty = findPartyName(userMessage.getPartyInfo().getFrom().getPartyId());
            LOG.businessInfo(DomibusMessageCode.BUS_PARTY_ID_FOUND, senderParty, userMessage.getPartyInfo().getFrom().getPartyId());
            receiverParty = findPartyName(userMessage.getPartyInfo().getTo().getPartyId());
            LOG.businessInfo(DomibusMessageCode.BUS_PARTY_ID_FOUND, receiverParty, userMessage.getPartyInfo().getTo().getPartyId());
            service = findServiceName(userMessage.getCollaborationInfo().getService());
            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_SERVICE_FOUND, service, userMessage.getCollaborationInfo().getService());
            action = findActionName(userMessage.getCollaborationInfo().getAction());
            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_ACTION_FOUND, action, userMessage.getCollaborationInfo().getAction());
            leg = findLegName(agreementName, senderParty, receiverParty, service, action);
            LOG.businessInfo(DomibusMessageCode.BUS_LEG_NAME_FOUND, leg, agreementName, senderParty, receiverParty, service, action);

            if ((action.equals(Ebms3Constants.TEST_ACTION) && (!service.equals(Ebms3Constants.TEST_SERVICE)))) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "ebMS3 Test Service: " + Ebms3Constants.TEST_SERVICE + " and ebMS3 Test Action: " + Ebms3Constants.TEST_ACTION + " can only be used together [CORE]", messageId, null);
            }

            MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration(agreementName, senderParty, receiverParty, service, action, leg);
            LOG.debug("Found pmodeKey [{}] for message [{}]", messageExchangeConfiguration.getPmodeKey(), userMessage);
            return messageExchangeConfiguration;

        } catch (IllegalStateException ise) {
            // It can happen if DB is clean and no pmodes are configured yet!
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "PMode could not be found. Are PModes configured in the database?", messageId, ise);
        }
    }


    class ReloadPmodeMessageCreator implements MessageCreator {
        @Override
        public Message createMessage(Session session) throws JMSException {
            Message m = session.createMessage();
            m.setStringProperty(Command.COMMAND, Command.RELOAD_PMODE);
            m.setStringProperty(MessageConstants.DOMAIN, domainContextProvider.getCurrentDomain().getCode());
            return m;
        }
    }

    public abstract List<String> getMpcList();

    public abstract List<String> getMpcURIList();

    protected abstract String findLegName(String agreementRef, String senderParty, String receiverParty, String service, String action) throws EbMS3Exception;

    protected abstract String findActionName(String action) throws EbMS3Exception;

    protected abstract String findServiceName(eu.domibus.ebms3.common.model.Service service) throws EbMS3Exception;

    protected abstract String findPartyName(Collection<PartyId> partyId) throws EbMS3Exception;

    protected abstract String findAgreement(AgreementRef agreementRef) throws EbMS3Exception;

    public abstract Party getGatewayParty();

    public abstract Party getSenderParty(String pModeKey);

    public abstract Party getReceiverParty(String pModeKey);

    public abstract Service getService(String pModeKey);

    public abstract Action getAction(String pModeKey);

    public abstract Agreement getAgreement(String pModeKey);

    public abstract LegConfiguration getLegConfiguration(String pModeKey);

    public abstract boolean isMpcExistant(String mpc);

    public abstract int getRetentionDownloadedByMpcName(String mpcName);

    public abstract int getRetentionDownloadedByMpcURI(final String mpcURI);

    public abstract int getRetentionUndownloadedByMpcName(String mpcName);

    public abstract int getRetentionUndownloadedByMpcURI(final String mpcURI);

    public abstract Role getBusinessProcessRole(String roleValue);

    protected String getSenderPartyNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[0];
    }

    protected String getReceiverPartyNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[1];
    }

    protected String getServiceNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[2];
    }

    protected String getActionNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[3];
    }

    protected String getAgreementRefNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[4];
    }

    protected String getLegConfigurationNameFromPModeKey(final String pModeKey) {
        return pModeKey.split(":")[5];
    }

    public abstract List<Process> findPullProcessesByMessageContext(final MessageExchangeConfiguration messageExchangeConfiguration);

    public abstract List<Process> findPullProcessesByInitiator(final Party party);

    public abstract List<Process> findPullProcessByMpc(final String mpc);

    public abstract List<Process> findAllProcesses();

    public abstract List<Party> findAllParties();

    public abstract List<String> findPartyIdByServiceAndAction(final String service, final String action);

    public abstract String getPartyIdType(String partyIdentifier);

    public abstract String getServiceType(String serviceValue);

    public abstract String getRole(String roleType, String serviceValue);

    public abstract String getAgreementRef(String serviceValue);

}
