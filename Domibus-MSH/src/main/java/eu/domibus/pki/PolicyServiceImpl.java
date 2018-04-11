
package eu.domibus.pki;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("cryptoService")
public class PolicyServiceImpl implements PolicyService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    DomibusConfigurationService domibusConfigurationService;


    /**
     * To retrieve the domibus security policy xml from the specified location and create the Security Policy object.
     *
     * @param location
     * @return
     * @throws ConfigurationException
     */
    @Override
    @Cacheable("policyCache")
    public Policy parsePolicy(final String location) throws ConfigurationException {
        final PolicyBuilder pb = BusFactory.getDefaultBus().getExtension(PolicyBuilder.class);
        try {
            return pb.getPolicy(new FileInputStream(new File(domibusConfigurationService.getConfigLocation(), location)));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Checks whether the security policy specified is a No Signature - No security policy.
     * If null is provided, a no security policy is assumed.
     * A no security policy would be used to avoid certificate validation.
     *
     * @param policy
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
