package eu.domibus.ebms3.receiver;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.metrics.Counter;
import eu.domibus.common.metrics.Timer;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.sender.DispatchClientDefaultProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static eu.domibus.common.metrics.MetricNames.OUTGOING_PULL_RECEIPT;
import static eu.domibus.common.metrics.MetricNames.SERVER_POLICY_IN;

/**
 * @author Thomas Dussart
 * @author Cosmin Baciu
 * @since 4.1
 */
public class SetPolicyInServerInterceptor extends SetPolicyInInterceptor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetPolicyInServerInterceptor.class);

    @Timer(SERVER_POLICY_IN)
    @Counter(SERVER_POLICY_IN)
    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        final String httpMethod = (String) message.get("org.apache.cxf.request.method");
        //TODO add the below logic to a separate interceptor
        if (StringUtils.containsIgnoreCase(httpMethod, HttpMethod.GET)) {
            LOG.debug("Detected GET request on MSH: aborting the interceptor chain");
            message.getInterceptorChain().abort();

            final HttpServletResponse response = (HttpServletResponse) message.get(AbstractHTTPDestination.HTTP_RESPONSE);
            response.setStatus(HttpServletResponse.SC_OK);
            try {
                response.getWriter().println(domibusPropertiesService.getDisplayVersion());
            } catch (IOException ex) {
                throw new Fault(ex);
            }
            return;
        }

        Messaging messaging = null;
        String policyName = null;
        String messageId = null;

        try {

            messaging = soapService.getMessage(message);
            message.put(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY, messaging);

            LegConfigurationExtractor legConfigurationExtractor = messageLegConfigurationFactory.extractMessageConfiguration(message, messaging);
            if (legConfigurationExtractor == null) return;

            final LegConfiguration legConfiguration = legConfigurationExtractor.extractMessageConfiguration();
            policyName = legConfiguration.getSecurity().getPolicy();
            Policy policy = policyService.parsePolicy("policies" + File.separator + policyName);

            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_USE, policyName);

            message.getExchange().put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.getInterceptorChain().add(new CheckEBMSHeaderInterceptor());
            message.getInterceptorChain().add(new SOAPMessageBuilderInterceptor());
            final String securityAlgorithm = legConfiguration.getSecurity().getSignatureMethod().getAlgorithm();
            message.put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            message.getExchange().put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_ALGORITHM_INCOMING_USE, securityAlgorithm);

        } catch (EbMS3Exception e) {
            setBindingOperation(message);
            LOG.debug("", e); // Those errors are expected (no PMode found, therefore DEBUG)
            throw new Fault(e);
        } catch (IOException | JAXBException e) {
            setBindingOperation(message);
            LOG.businessError(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_NOT_FOUND, e, policyName); // Those errors are not expected
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "no valid security policy found", messaging != null ? messageId : "unknown", e);
            ex.setMshRole(MSHRole.RECEIVING);
            throw new Fault(ex);
        }
    }

}
