package eu.domibus.core.proxy;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Injectable;
import mockit.NonStrictExpectations;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

/**
 * @author idragusa
 * @since 4.1
 */
@RunWith(JMockit.class)
public class DomibusProxyServiceImplTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusProxyServiceImplTest.class);

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    DomibusProxyServiceImpl domibusProxyService;

    @Test(expected = DomibusCoreException.class)
    public void initDomibusProxyMissingHostTest() {
        new NonStrictExpectations(){{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

        }};
        domibusProxyService.initDomibusProxy();
    }

    @Test(expected = DomibusCoreException.class)
    public void initDomibusProxyMissingPortTest() {
        new NonStrictExpectations(){{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

        }};
        domibusProxyService.initDomibusProxy();
    }

    @Test
    public void initDomibusProxyTest() {
        new NonStrictExpectations(){{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

        }};
        domibusProxyService.initDomibusProxy();
    }

    @Test(expected = DomibusCoreException.class)
    public void initDomibusProxyMissingPasswordTest() {
        new NonStrictExpectations(){{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_USER);
            result = "idragusa";

        }};
        domibusProxyService.initDomibusProxy();
    }

    @Test
    public void initDomibusProxyAuthTest() {
        new NonStrictExpectations(){{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_USER);
            result = "idragusa";

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_PASSWORD);
            result = "pass";

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_NON_PROXY_HOSTS);
            result = "localhost";


        }};
        domibusProxyService.initDomibusProxy();
        Assert.assertTrue(domibusProxyService.getDomibusProxy().isEnabled());
        Assert.assertEquals("12.13.14.15", domibusProxyService.getDomibusProxy().getHttpProxyHost());
        Assert.assertEquals(new Integer("8012"), domibusProxyService.getDomibusProxy().getHttpProxyPort());
        Assert.assertEquals("idragusa", domibusProxyService.getDomibusProxy().getHttpProxyUser());
        Assert.assertEquals("pass", domibusProxyService.getDomibusProxy().getHttpProxyPassword());
        Assert.assertEquals("localhost", domibusProxyService.getDomibusProxy().getNonProxyHosts());
    }
}


