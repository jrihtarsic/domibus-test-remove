package eu.domibus.pki;

import eu.domibus.api.util.HttpUtil;
import eu.domibus.util.HttpUtilImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Cosmin Baciu on 07-Jul-16.
 */
@RunWith(JMockit.class)
public class CRLUtilTest {

    @Tested
    CRLUtil crlUtil;

    @Injectable
    HttpUtil httpUtil;

    PKIUtil pkiUtil = new PKIUtil();

    @Before
    public void init() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test(expected = DomibusCRLException.class)
    public void testDownloadCRLWithNullUrl(@Mocked final URL crlUrl) throws Exception {
        final String crlUrlString = "file://test";
        new Expectations(crlUtil) {{
            crlUtil.getCrlURL(crlUrlString);
            result = null;
        }};

        crlUtil.downloadCRL(crlUrlString);
    }

    @Test
    public void testDownloadCRL(@Mocked final URL crlUrl) throws Exception {
        X509CRL crl = pkiUtil.createCRL(Arrays.asList(new BigInteger("0400000000011E44A5E405", 16),
                new BigInteger("0400000000011E44A5E404", 16)));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(crl.getEncoded());

        final String crlUrlString = "file://test";
        new Expectations(crlUtil) {{
            crlUrl.openStream();
            result = inputStream;

            crlUtil.getCrlURL(crlUrlString);
            result = crlUrl;
        }};

        X509CRL x509CRL = crlUtil.downloadCRL(crlUrlString);
        assertNotNull(x509CRL);
        assertEquals(crl, x509CRL);
    }

    @Test
    public void testGetCrlUrl() throws Exception {
        //prepare
        final String classPathResource = "test.crl";
        new Expectations(crlUtil) {{
            crlUtil.getResourceFromClasspath(classPathResource);
            result = new URL("file://test");
        }};

        //test
        URL httpCrlURL = crlUtil.getCrlURL("http://domain.crl");
        assertNotNull(httpCrlURL);
        URL httpsCrlURL = crlUtil.getCrlURL("https://domain.crl");
        assertNotNull(httpsCrlURL);
        URL ftpCrlURL = crlUtil.getCrlURL("ftp://domain.crl");
        assertNotNull(ftpCrlURL);
        URL fileCrlURL = crlUtil.getCrlURL("file://domain.crl");
        assertNotNull(fileCrlURL);

        URL classpathCrlURL = crlUtil.getCrlURL(classPathResource);
        assertNotNull(classpathCrlURL);
    }

    @Test
    public void testGetCRLDistributionPoints() throws Exception {
        //prepare
        String crlUrl1 = "http://domain1.crl";
        String crlUrl2 = "http://domain2.crl";
        List<String> crlUrlList = Arrays.asList(crlUrl1, crlUrl2);
        X509Certificate certificate = pkiUtil.createCertificate(BigInteger.ONE, crlUrlList);

        //test
        List<String> crlDistributionPoints = crlUtil.getCrlDistributionPoints(certificate);

        //assert
        assertNotNull(crlDistributionPoints);
        assertEquals(crlUrlList, crlDistributionPoints);
        System.out.println(crlDistributionPoints);
    }

    //@Test
    public void testDownloadCRLFromLDAP() throws Exception {
        final String ldapURL = "ldap://ldap.sbca.telesec.de/CN=Shared%20Business%20CA%204,OU=T-Systems%20Trust%20Center,O=T-Systems%20International%20GmbH,C=DE?CertificateRevocationList";

        //final String ldapURL = "ldap://ldap.infonotary.com/dc=identity-ca,dc=infonotary,dc=com";

        X509CRL x509CRL = crlUtil.downloadCRLfromLDAP(ldapURL);
        Assert.assertNotNull(x509CRL);

        System.out.println(x509CRL);
    }

    @Test(expected = DomibusCRLException.class)
    public void testDownloadCRLFromLDAP_WrongUrl() throws Exception {
        //ldap url contains spaces and it will fail
        final String ldapURL = "ldap://ldap.sbca.telesec.de/CN=Shared Business CA 04,OU=T-Systems Trust Center,O=T-Systems International GmbH,C=DE?CertificateRevocationList";

        X509CRL x509CRL = crlUtil.downloadCRLfromLDAP(ldapURL);
    }


}
