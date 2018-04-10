package eu.domibus.pki;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.security.TrustStoreEntry;

import javax.naming.InvalidNameException;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @Author Cosmin Baciu
 * @Since 3.2
 */
public interface CertificateService {

    boolean isCertificateValid(X509Certificate cert) throws DomibusCertificateException;

    boolean isCertificateChainValid(KeyStore trustStore, String alias);

    String extractCommonName(final X509Certificate certificate) throws InvalidNameException;

    X509Certificate loadCertificateFromJKSFile(String filePath, String alias, String password);

    /**
     * Returne the detail of the truststore entries.
     *
     * @return a list of certificate
     */
    List<TrustStoreEntry> getTrustStoreEntries(final KeyStore trustStore);

    /**
     * Save certificate data in the database, and use this data to display a revocation warning when needed.
     */
    void saveCertificateAndLogRevocation(Domain domain);

    void validateLoadOperation(ByteArrayInputStream newTrustStoreBytes, String password);
}
