
package eu.domibus.pki;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Arun Raj
 * @since 3.3
 */
@Service
public class PolicyServiceImpl implements PolicyService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PolicyServiceImpl.class);
    public static final String POLICIES = "policies";

    @Autowired
    private DomibusConfigurationService domibusConfigurationService;

    @Autowired
    private Bus bus;


    /**
     * To retrieve the domibus security policy xml from the specified location and create the Security Policy object.
     *
     * @param location the policy xml file location
     * @return the security policy
     * @throws ConfigurationException if the policy xml cannot be read or parsed from the file
     */
    @Override
    @Cacheable("policyCache")
    public Policy parsePolicy(final String location) throws ConfigurationException {
        final PolicyBuilder pb = bus.getExtension(PolicyBuilder.class);
        try {
            return pb.getPolicy(new FileInputStream(new File(domibusConfigurationService.getConfigLocation(), location)));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Policy getPolicy(LegConfiguration legConfiguration) throws ConfigurationException {
        return parsePolicy(POLICIES + File.separator + legConfiguration.getSecurity().getPolicy());
    }

    /**
     * Checks whether the security policy specified is a No Signature - No security policy.
     * If null is provided, a no security policy is assumed.
     * A no security policy would be used to avoid certificate validation.
     *
     * @param policy the security policy
     * @return boolean
     */
    @Override
    public boolean isNoSecurityPolicy(Policy policy) {

        if (null == policy) {
            LOG.securityWarn(DomibusMessageCode.SEC_NO_SECURITY_POLICY_USED, "Security policy provided is null! Assuming no security policy - no signature is specified!");
            return true;
        } else if (policy.isEmpty()) {
            LOG.securityWarn(DomibusMessageCode.SEC_NO_SECURITY_POLICY_USED, "Policy components are empty! No security policy specified!");
            return true;
        } else {
            return false;
        }
    }
}
