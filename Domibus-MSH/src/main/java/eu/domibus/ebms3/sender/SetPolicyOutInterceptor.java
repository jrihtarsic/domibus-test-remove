package eu.domibus.ebms3.sender;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.pki.PolicyService;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyInInterceptor;
import org.apache.cxf.ws.policy.PolicyVerificationOutInterceptor;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.SOAPMessage;

/**
 * This interceptor is responsible for discovery and setup of WS-Security Policies for outgoing messages
 *
 * @author Christian Koch, Stefan Mueller
 * @since 3.0
 */
public class SetPolicyOutInterceptor extends AbstractSoapInterceptor {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetPolicyOutInterceptor.class);

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private DomibusConfigurationService domibusConfigurationService;


    public SetPolicyOutInterceptor() {
        super(Phase.SETUP);
        this.addBefore(PolicyInInterceptor.class.getName());
    }

    /**
     * Intercepts a message.
     * Interceptors should NOT invoke handleMessage or handleFault
     * on the next interceptor - the interceptor chain will
     * take care of this.
     *
     * @param message the message to handle
     */
    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        LOG.debug("SetPolicyOutInterceptor");
        final String pModeKey = (String) message.getContextualProperty(DispatchClientDefaultProvider.PMODE_KEY_CONTEXT_PROPERTY);
        LOG.debug("Using pmodeKey [{}]", pModeKey);
        message.getInterceptorChain().add(new PrepareAttachmentInterceptor());

        final LegConfiguration legConfiguration = this.pModeProvider.getLegConfiguration(pModeKey);

        message.put(SecurityConstants.USE_ATTACHMENT_ENCRYPTION_CONTENT_ONLY_TRANSFORM, true);

        final String securityAlgorithm = legConfiguration.getSecurity().getSignatureMethod().getAlgorithm();
        message.put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
        LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_ALGORITHM_OUTGOING_USE, securityAlgorithm);
        String encryptionUsername = null;
        try {
            Party receiverParty = pModeProvider.getReceiverParty(pModeKey);
            if(receiverParty != null) {
                encryptionUsername = receiverParty.getName();
            }
        } catch (ConfigurationException exc) {
            LOG.info("Initiator party was not found, will be extracted from pModeKey.");
        }
        if(encryptionUsername == null) {
            encryptionUsername = pModeProvider.getReceiverPartyNameFromPModeKey(pModeKey);
        }

        message.put(SecurityConstants.ENCRYPT_USERNAME, encryptionUsername);
        LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_USER_OUTGOING_USE, encryptionUsername);

        try {
            final Policy policy = policyService.parsePolicy("policies/" + legConfiguration.getSecurity().getPolicy());
            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_POLICY_OUTGOING_USE, legConfiguration.getSecurity().getPolicy());
            message.put(PolicyConstants.POLICY_OVERRIDE, policy);
        } catch (final ConfigurationException e) {
            LOG.businessError(DomibusMessageCode.BUS_SECURITY_POLICY_OUTGOING_NOT_FOUND, e, legConfiguration.getSecurity().getPolicy());
            throw new Fault(new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "Could not find policy file " + domibusConfigurationService.getConfigLocation() + "/" + this.pModeProvider.getLegConfiguration(pModeKey).getSecurity(), null, null));
        }

    }


    public static class LogAfterPolicyCheckInterceptor extends AbstractSoapInterceptor {


        public LogAfterPolicyCheckInterceptor() {
            super(Phase.POST_STREAM);
            this.addAfter(PolicyVerificationOutInterceptor.class.getName());
        }

        @Override
        public void handleMessage(final SoapMessage message) throws Fault {

            final SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            soapMessage.removeAllAttachments();
        }
    }
}
