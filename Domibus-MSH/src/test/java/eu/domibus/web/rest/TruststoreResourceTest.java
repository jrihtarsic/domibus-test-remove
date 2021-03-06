package eu.domibus.web.rest;

import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.services.DomibusCacheService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.TrustStoreRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static eu.domibus.web.rest.TruststoreResource.ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@RunWith(JMockit.class)
public class TruststoreResourceTest {

    @Tested
    TruststoreResource truststoreResource;

    @Injectable
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    protected DomainContextProvider domainProvider;

    @Injectable
    DomibusCacheService domibusCacheService;

    @Injectable
    CertificateService certificateService;

    @Injectable
    DomainCoreConverter domainConverter;

    @Injectable
    private CsvServiceImpl csvServiceImpl;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Test
    public void testUploadTruststoreFileSuccess() throws IOException {
        // Given
        MultipartFile multiPartFile = new MockMultipartFile("filename", new byte[]{1, 0, 1});

        // When
        ResponseEntity<String> responseEntity = truststoreResource.uploadTruststoreFile(multiPartFile, "pass");

        // Then
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assert.assertEquals("Truststore file has been successfully replaced.", responseEntity.getBody());
    }

    @Test
    public void testUploadTruststoreEmpty() throws IOException {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("emptyfile", new byte[]{});

        // When
        ResponseEntity<String> responseEntity = truststoreResource.uploadTruststoreFile(emptyFile, "pass");

        // Then
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assert.assertEquals("Failed to upload the truststore file since it was empty.", responseEntity.getBody());
    }

    @Test(expected = CryptoException.class)
    public void testUploadTruststoreException() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        // Given
        MultipartFile multiPartFile = new MockMultipartFile("filename", new byte[]{1, 0, 1});

        final Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;

            multiDomainCertificateProvider.replaceTrustStore(domain, anyString, (byte[]) any, anyString);
            result = new CryptoException("Password is incorrect");
        }};

        // When
        ResponseEntity<String> responseEntity = truststoreResource.uploadTruststoreFile(multiPartFile, "pass");
    }

    private List<TrustStoreRO> getTestTrustStoreROList(Date date) {
        List<TrustStoreRO> trustStoreROList = new ArrayList<>();
        TrustStoreRO trustStoreRO = new TrustStoreRO();
        trustStoreRO.setName("Name");
        trustStoreRO.setSubject("Subject");
        trustStoreRO.setIssuer("Issuer");
        trustStoreRO.setValidFrom(date);
        trustStoreRO.setValidUntil(date);
        trustStoreROList.add(trustStoreRO);
        return trustStoreROList;
    }

    @Test
    public void testTrustStoreEntries(@Mocked KeyStore trustStore) {
        // Given
        Date date = new Date();
        List<TrustStoreEntry> trustStoreEntryList = new ArrayList<>();
        TrustStoreEntry trustStoreEntry = new TrustStoreEntry("Name", "Subject", "Issuer", date, date);
        trustStoreEntryList.add(trustStoreEntry);

        final Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;

            multiDomainCertificateProvider.getTrustStore(domain);
            result = trustStore;

            certificateService.getTrustStoreEntries(trustStore);
            result = trustStoreEntryList;
            domainConverter.convert(trustStoreEntryList, TrustStoreRO.class);
            result = getTestTrustStoreROList(date);
        }};

        // When
        final List<TrustStoreRO> trustStoreROList = truststoreResource.trustStoreEntries();

        // Then
        Assert.assertEquals(getTestTrustStoreROList(date), trustStoreROList);
    }

    @Test
    public void testGetCsv() throws EbMS3Exception {
        // Given
        Date date = new Date();
        List<TrustStoreRO> trustStoreROList = getTestTrustStoreROList(date);
        new Expectations(truststoreResource) {{
            truststoreResource.trustStoreEntries();
            result = trustStoreROList;
            csvServiceImpl.exportToCSV(trustStoreROList, null, (Map<String, String>) any, (List<String>) any);
            result = "Name, Subject, Issuer, Valid From, Valid Until" + System.lineSeparator() +
                    "Name, Subject, Issuer, " + date + ", " + date + System.lineSeparator();
        }};

        // When
        final ResponseEntity<String> csv = truststoreResource.getCsv();

        // Then
        Assert.assertEquals(HttpStatus.OK, csv.getStatusCode());
        Assert.assertEquals("Name, Subject, Issuer, Valid From, Valid Until" + System.lineSeparator() +
                        "Name, Subject, Issuer, " + date + ", " + date + System.lineSeparator(),
                csv.getBody());
    }

    @Test
    public void uploadTruststoreFile_rejectsWhenNoPasswordProvided(@Injectable  MultipartFile multipartFile) throws Exception {
        // GIVEN
        final String emptyPassword = "";

        // WHEN
        ResponseEntity<String> response = truststoreResource.uploadTruststoreFile(multipartFile, emptyPassword);

        // THEN
        Assert.assertEquals("Should have rejected the request as bad", HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assert.assertTrue("Should have returned the correct error message", response.getBody().contentEquals(ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD));
    }
}
