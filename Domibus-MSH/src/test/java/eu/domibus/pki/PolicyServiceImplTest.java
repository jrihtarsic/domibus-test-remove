package eu.domibus.pki;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Security;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.apache.cxf.Bus;
import org.apache.neethi.Policy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Arun Raj
 * @since 3.3
 */
@RunWith(JMockit.class)
public class PolicyServiceImplTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PolicyServiceImplTest.class);
    private static final String TEST_RESOURCES_DIR = "./src/test/resources";
    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    Bus bus;

    @Tested
    PolicyServiceImpl policyService;

    @Test
    public void testIsNoSecurityPolicy_NullPolicy() {
        //when null policy is specified
        boolean result1 = policyService.isNoSecurityPolicy(null);
        Assert.assertTrue("Expected NoSecurityPolicy as true when null input provided", result1 == true);
    }

    @Test
    public void testIsNoSecurityPolicy_DoNothingPolicy(@Injectable final Policy doNothingPolicy) {
        //when doNothingPolicy.xml is specified
        new Expectations() {{
            doNothingPolicy.isEmpty();
            result = true;
        }};

        boolean result2 = policyService.isNoSecurityPolicy(doNothingPolicy);
        Assert.assertTrue(result2 == true);
    }

    @Test
    public void testIsNoSecurityPolicy_SignOnPolicy(@Injectable final Policy signOnlyPolicy) {
        new Expectations() {{
            signOnlyPolicy.isEmpty();
            result = false;
        }};

        boolean result3 = policyService.isNoSecurityPolicy(signOnlyPolicy);
        Assert.assertTrue(result3 == false);
    }

    @Test
    public void testParsePolicyException() {

        try {
            policyService.parsePolicy("NonExistentFileLocation");
        } catch (Exception e) {
            Assert.assertTrue("Expecting ConfigurationException", e instanceof ConfigurationException);
        }
    }

    @Test
    public void testGetPolicy() {
        LegConfiguration legConfiguration = new LegConfiguration();
        Security securityPolicy = new Security();
        securityPolicy.setPolicy("NonExistentPolicy");
        legConfiguration.setSecurity(securityPolicy);

        try {
            policyService.getPolicy(legConfiguration);
        } catch (Exception e) {
            Assert.assertTrue("Expecting ConfigurationException", e instanceof ConfigurationException);
        }
    }

}
