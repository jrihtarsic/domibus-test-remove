package eu.domibus.ebms3.receiver;

import eu.domibus.ebms3.SoapInterceptorTest;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.pki.CertificateService;
import eu.domibus.pki.PKIUtil;
import eu.domibus.spring.SpringContextProvider;
import eu.domibus.wss4j.common.crypto.CryptoService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsOperations;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * @author idragusa
 * @since 4.0
 */
@RunWith(JMockit.class)
public class TrustSenderInterceptorTest extends SoapInterceptorTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustSenderInterceptorTest.class);

    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/receiver/";

    @Injectable
    CertificateService certificateService;

    @Injectable
    protected JAXBContext jaxbContextEBMS;

    @Tested
    TrustSenderInterceptor trustSenderInterceptor;

    @Bean
    @Qualifier("jmsTemplateCommand")
    public JmsOperations jmsOperations() throws JAXBException {
        return Mockito.mock(JmsOperations.class);
    }

    PKIUtil pkiUtil = new PKIUtil();

    @Test
    public void testHandleMessageKeyIdentifier(@Mocked SpringContextProvider springContextProvider) throws XMLStreamException, ParserConfigurationException, JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequest.xml");
        String trustoreFilename = RESOURCE_PATH + "gateway_truststore.jks";
        String trustorePassword = "test123";

        testHandleMessage(doc, trustoreFilename, trustorePassword);
    }

    @Test
    public void testHandleMessageBinaryToken(@Mocked SpringContextProvider springContextProvider) throws XMLStreamException, ParserConfigurationException, JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");
        String trustoreFilename = RESOURCE_PATH + "nonEmptySource.jks";
        String trustorePassword = "1234";

        testHandleMessage(doc, trustoreFilename, trustorePassword);
    }

    @Test(expected = org.apache.cxf.interceptor.Fault.class)
    public void testSenderTrustFault(@Mocked SpringContextProvider springContextProvider) throws XMLStreamException, ParserConfigurationException, JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");
        String trustoreFilename = RESOURCE_PATH + "nonEmptySource.jks";
        String trustorePassword = "1234";

        new Expectations() {{
            certificateService.isCertificateValid((X509Certificate)any);
            result = false;
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING, "true");
            result = true;
        }};
        testHandleMessage(doc, trustoreFilename, trustorePassword);
    }

    @Test
    public void testSenderTrustNoSenderVerification(@Mocked SpringContextProvider springContextProvider) throws XMLStreamException, ParserConfigurationException, JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");
        String trustoreFilename = RESOURCE_PATH + "nonEmptySource.jks";
        String trustorePassword = "1234";

        new Expectations() {{
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, "false");
            result = false;
        }};
        testHandleMessage(doc, trustoreFilename, trustorePassword);
        new Verifications() {{
            certificateService.isCertificateValid((X509Certificate)any); times = 0;
        }};
    }

    @Test
    public void testGetCertificateFromBinarySecurityToken() throws XMLStreamException, ParserConfigurationException, WSSecurityException, CertificateException {
        Document doc = readDocument("dataset/as4/RawXMLMessageWithSpaces.xml");
        X509Certificate xc = trustSenderInterceptor.getCertificateFromBinarySecurityToken(doc.getDocumentElement());
        Assert.assertNotNull(xc);
        Assert.assertNotNull(xc.getIssuerDN());
    }

    protected void testHandleMessage(Document doc, String trustoreFilename,  String trustorePassword) throws JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        SoapMessage soapMessage = getSoapMessageForDom(doc);
        Properties properties = createTruststoreProperties(trustoreFilename, trustorePassword);
        trustSenderInterceptor.setSecurityEncryptionProp(properties);
        byte[] sourceTrustore = FileUtils.readFileToByteArray(new File(trustoreFilename));
        CryptoService cryptoService = createCryptoService(sourceTrustore, trustorePassword, properties);
        new Expectations() {{
            SpringContextProvider.getApplicationContext().getBean("cryptoService");
            result = cryptoService;
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, "false");
            result = true;
        }};
        trustSenderInterceptor.handleMessage(soapMessage);
    }

    @Test
    public void testCheckCertificateValidityEnabled() throws Exception {
        final X509Certificate certificate = pkiUtil.createCertificate(BigInteger.ONE, null);
        final X509Certificate expiredCertificate = pkiUtil.createCertificate(BigInteger.ONE, new DateTime().minusDays(2).toDate(), new DateTime().minusDays(1).toDate(), null);

        new Expectations() {{
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING, "true");
            result = "true";
            certificateService.isCertificateValid(certificate);
            result = true;
            certificateService.isCertificateValid(expiredCertificate);
            result = false;

        }};

        Assert.assertTrue(trustSenderInterceptor.checkCertificateValidity(certificate, "test sender", false));
        Assert.assertFalse(trustSenderInterceptor.checkCertificateValidity(expiredCertificate, "test sender", false));
    }

    @Test
    public void testCheckCertificateValidityDisabled() throws Exception {
        final X509Certificate expiredCertificate = pkiUtil.createCertificate(BigInteger.ONE, new DateTime().minusDays(2).toDate(), new DateTime().minusDays(1).toDate(), null);

        new Expectations() {{
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING, "true");
            result = "false";
        }};
        Assert.assertTrue(trustSenderInterceptor.checkCertificateValidity(expiredCertificate, "test sender", false));
    }

    @Test
    public void testCheckSenderPartyTrust() throws Exception {
        final X509Certificate certificate = pkiUtil.createCertificate(BigInteger.ONE, null);

        new Expectations() {{
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, "false");
            result = "true";
        }};

        Assert.assertTrue(trustSenderInterceptor.checkSenderPartyTrust(certificate, "GlobalSign", "messageID123", false));
        Assert.assertFalse(trustSenderInterceptor.checkSenderPartyTrust(certificate, "test sender", "messageID123", false));
    }

    @Test
    public void testCheckSenderPartyTrustDisabled() throws Exception {
        final X509Certificate certificate = pkiUtil.createCertificate(BigInteger.ONE, null);

        new Expectations() {{
            domibusProperties.getProperty(TrustSenderInterceptor.DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, "false");
            result = "false";
        }};

        Assert.assertTrue(trustSenderInterceptor.checkSenderPartyTrust(certificate, "test sender", "messageID123", false));
    }

    protected Properties createTruststoreProperties(final String filename, final String password) {
        Properties prop = new Properties();

        prop.setProperty("org.apache.ws.security.crypto.merlin.trustStore.type", "jks");
        prop.setProperty("org.apache.ws.security.crypto.merlin.load.cacerts", "false");
        prop.setProperty("org.apache.ws.security.crypto.provider", "eu.domibus.wss4j.common.crypto.Merlin");
        prop.setProperty("org.apache.ws.security.crypto.merlin.trustStore.file", filename);
        prop.setProperty("org.apache.ws.security.crypto.merlin.trustStore.password", password);

        return prop;
    }

    protected CryptoService createCryptoService(byte[] sourceTrustore, String trustorePassword, Properties properties ) throws JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        CryptoService cryptoService = new CryptoService();
        cryptoService.setTrustStoreProperties(properties);
        cryptoService.setJmsOperations(jmsOperations());
        cryptoService.replaceTruststore(sourceTrustore, trustorePassword);

        return cryptoService;
    }
}