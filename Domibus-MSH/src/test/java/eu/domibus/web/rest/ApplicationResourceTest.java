package eu.domibus.web.rest;

import eu.domibus.common.util.DomibusPropertiesService;
import eu.domibus.web.rest.ro.DomibusInfoRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Hashtable;
import java.util.Properties;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@RunWith(JMockit.class)
public class ApplicationResourceTest {

    private static final String DOMIBUS_VERSION = "Domibus Unit Tests";

    @Tested
    private ApplicationResource applicationResource;

    @Injectable
    private DomibusPropertiesService domibusPropertiesService;

    @Injectable
    private Properties domibusProperties;

    @Test
    public void testGetDomibusInfo() throws Exception {
        // Given
        new Expectations() {{
            domibusPropertiesService.getDisplayVersion();
            result = DOMIBUS_VERSION;
        }};

        // When
        DomibusInfoRO domibusInfo = applicationResource.getDomibusInfo();

        // Then
        Assert.assertNotNull(domibusInfo);
        Assert.assertEquals(DOMIBUS_VERSION, domibusInfo.getVersion());
    }

    @Test
    public void testGetFourCornerEnabled() throws Exception {

        new Expectations() {{
            domibusProperties.getProperty(ApplicationResource.FOURCORNERMODEL_ENABLED_KEY, anyString);
            result = "false";
        }};

        //tested method
        boolean isFourCornerEnabled = applicationResource.getFourCornerModelEnabled();

        Assert.assertEquals(false, isFourCornerEnabled);
    }
}
