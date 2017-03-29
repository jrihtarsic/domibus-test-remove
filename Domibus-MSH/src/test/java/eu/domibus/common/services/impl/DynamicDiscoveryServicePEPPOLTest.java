package eu.domibus.common.services.impl;

import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.services.DynamicDiscoveryService;
import eu.domibus.common.services.impl.DynamicDiscoveryServicePEPPOL;
import eu.domibus.common.util.EndpointInfo;
import eu.domibus.common.util.TrustoreUtil;
import mockit.*;
import mockit.integration.junit4.JMockit;
import no.difi.vefa.edelivery.lookup.LookupClient;
import no.difi.vefa.edelivery.lookup.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JMockit.class)
public class DynamicDiscoveryServicePEPPOLTest {

    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/";

    private static final String TEST_KEYSTORE = "testkeystore.jks";

    private static final String TEST_SML_ZONE = "acc.edelivery.tech.ec.europa.eu";

    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String TEST_KEYSTORE_PASSWORD = "1234";

    private static final String TEST_RECEIVER_ID = "unknownRecipient";
    private static final String TEST_RECEIVER_ID_TYPE = "unknownRecipientType";
    private static final String TEST_ACTION_VALUE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:www.cenbii.eu:transaction:biitrns014:ver2.0:extended:urn:www.peppol.eu:bis:peppol5a:ver2.0::2.1";
    private static final String TEST_SERVICE_VALUE = "serviceValue";
    private static final String TEST_SERVICE_TYPE = "serviceType";
    private static final String TEST_INVALID_SERVICE_VALUE = "invalidServiceValue";

    private static final String ADDRESS = "http://localhost:9090/anonymous/msh";

    @Injectable
    private Properties domibusProperties;

    @Tested
    private DynamicDiscoveryServicePEPPOL dynamicDiscoveryService;

    @Test
    public void testLookupInformation() throws Exception {
        new NonStrictExpectations() {{
            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
            result = TEST_SML_ZONE;

        }};

        EndpointInfo endpoint = dynamicDiscoveryService.lookupInformation("0088:2111100000666", "iso6523-actorid-upis", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-12::Invoice##urn:www.cenbii.eu:transaction:biicoretrdm010:ver1.0:#urn:www.peppol.eu:bis:peppol5a:ver2.0::2.1", "urn:www.cenbii.eu:profile:bii05:ver2.0", "cenbii-procid-ubl");
        assertNotNull(endpoint);

    }


    @Test
    public void testLookupInformationMock(final @Capturing LookupClient smpClient) throws Exception {
        new NonStrictExpectations() {{
            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
            result = TEST_SML_ZONE;

            ServiceMetadata sm = buildServiceMetadata();
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentIdentifier)any);
            result = sm;

        }};

        EndpointInfo endpoint = dynamicDiscoveryService.lookupInformation(TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
        assertNotNull(endpoint);
        assertEquals(ADDRESS, endpoint.getAddress());

        new Verifications() {{
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentIdentifier)any);
        }};
    }

    @Test(expected = ConfigurationException.class)
    public void testLookupInformationNotFound(final @Capturing LookupClient smpClient) throws Exception {
        new NonStrictExpectations() {{
            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
            result = TEST_SML_ZONE;
            ServiceMetadata sm = buildServiceMetadata();
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentIdentifier)any);
            result = sm;

        }};

        dynamicDiscoveryService.lookupInformation(TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_INVALID_SERVICE_VALUE, TEST_SERVICE_TYPE);
    }


    private ServiceMetadata buildServiceMetadata() {

        ServiceMetadata sm = new ServiceMetadata();
        X509Certificate testData = TrustoreUtil.loadCertificateFromJKS(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);

        ProcessIdentifier processIdentifier = new ProcessIdentifier(TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);

        Endpoint endpoint = new Endpoint(processIdentifier, new TransportProfile(DynamicDiscoveryService.transportProfileAS4), ADDRESS, testData);
        sm.addEndpoint(endpoint);

        return sm;
    }

}
