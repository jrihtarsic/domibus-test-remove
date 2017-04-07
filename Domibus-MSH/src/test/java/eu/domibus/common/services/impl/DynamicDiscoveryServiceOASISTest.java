package eu.domibus.common.services.impl;

import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.util.EndpointInfo;
import eu.domibus.wss4j.common.crypto.CryptoService;
import eu.europa.ec.dynamicdiscovery.DynamicDiscovery;
import eu.europa.ec.dynamicdiscovery.core.fetcher.FetcherResponse;
import eu.europa.ec.dynamicdiscovery.model.*;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oasis_open.docs.bdxr.ns.smp._2014._07.ServiceInformationType;
import org.oasis_open.docs.bdxr.ns.smp._2014._07.ServiceMetadataType;
import org.oasis_open.docs.bdxr.ns.smp._2014._07.SignedServiceMetadataType;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by idragusa on 3/27/17.
 */
@RunWith(JMockit.class)
public class DynamicDiscoveryServiceOASISTest {

    private static final String TEST_SML_ZONE = "ehealth.acc.edelivery.tech.ec.europa.eu";

    private static final String TEST_KEYSTORE_PASSWORD = "ehealth";

    private static final String TEST_RECEIVER_ID = "urn:romania:ncpb";
    private static final String TEST_RECEIVER_ID_TYPE = "ehealth-actorid-qns";
    private static final String TEST_ACTION_VALUE = "ehealth-resid-qns:urn::epsos##services:extended:epsos::107";
    private static final String TEST_SERVICE_VALUE = "urn:epsosPatientService::List";
    private static final String TEST_SERVICE_TYPE = "ehealth-procid-qns";
    private static final String TEST_INVALID_SERVICE_VALUE = "invalidServiceValue";

    private static final String ADDRESS = "http://localhost:9090/anonymous/msh";

    @Injectable
    private Properties domibusProperties;

    @Injectable
    private CryptoService cryptoService;

    @Tested
    DynamicDiscoveryServiceOASIS dynamicDiscoveryServiceOASIS;

    @Test
    public void testLookupInformationMock(final @Capturing DynamicDiscovery smpClient) throws Exception {
        new NonStrictExpectations() {{
            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
            result = TEST_SML_ZONE;
        }};

        //ServiceMetadata sm = buildServiceMetadata();


        EndpointInfo endpoint = dynamicDiscoveryServiceOASIS.lookupInformation(TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
        assertNotNull(endpoint);
        assertEquals(ADDRESS, endpoint.getAddress());

        new Verifications() {{
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentIdentifier)any);
        }};
    }

//    @Test(expected = ConfigurationException.class)
//    public void testLookupInformationNotFound(final @Capturing DynamicDiscovery smpClient) throws Exception {
//        new NonStrictExpectations() {{
//            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
//            result = TEST_SML_ZONE;
//            ServiceMetadataType sm = buildServiceMetadata();
//            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentIdentifier)any);
//            result = sm;
//
//        }};
//
//        dynamicDiscoveryServiceOASIS.lookupInformation(TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_INVALID_SERVICE_VALUE, TEST_SERVICE_TYPE);
//    }

//    private ServiceMetadataType buildServiceMetadata() throws Exception {
//        InputStream inputStream = getClass().getResourceAsStream("../ServiceMetadataResponseOASIS.xml");
//        FetcherResponse fetcherResponse = new FetcherResponse(inputStream, "http://docs.oasis-open.org/bdxr/ns/SMP/2016/05");
//        Object result = unmarshal(fetcherResponse);
//        ServiceInformationType serviceInformationType = ((ServiceMetadataType) result).getServiceInformation();
//        ServiceMetadataType serviceMetadata = new ServiceMetadataType();
//        serviceMetadata.setServiceInformation(serviceInformationType);
//
//        return serviceMetadata;
//    }

    private Object unmarshal(FetcherResponse fetcherResponse) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(ServiceMetadataType.class, SignedServiceMetadataType.class);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document document = documentBuilderFactory.newDocumentBuilder().parse(fetcherResponse.getInputStream());
        return ((JAXBElement)jaxbContext.createUnmarshaller().unmarshal(document)).getValue();
    }

    @Test // Hi Pawel and Flavio, this test is for you
    //@Ignore
    public void testLookupInformation() throws Exception {
        new NonStrictExpectations() {{
            domibusProperties.getProperty(DynamicDiscoveryServicePEPPOL.SMLZONE_KEY);
            result = TEST_SML_ZONE;

            KeyStore truststore;
            truststore = KeyStore.getInstance("JKS");
            truststore.load(getClass().getResourceAsStream("../ehealth_smp_acc_truststore.jks"), TEST_KEYSTORE_PASSWORD.toCharArray());

            cryptoService.getTrustStore();
            result = truststore;

        }};

        EndpointInfo endpointInfo = dynamicDiscoveryServiceOASIS.lookupInformation("urn:romania:ncpb", "ehealth-actorid-qns", "ehealth-resid-qns::urn::epsos##services:extended:epsos::107", "urn:www.cenbii.eu:profile:bii04:ver1.0", "cenbii-procid-ubl");
        System.out.println(endpointInfo.getAddress());
        Assert.assertNotNull(endpointInfo);
        Assert.assertEquals("http://localhost:8180/domibus/services/msh ", endpointInfo.getAddress());
    }
}
