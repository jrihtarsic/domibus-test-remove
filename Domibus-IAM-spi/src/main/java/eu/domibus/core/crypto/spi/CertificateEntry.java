package eu.domibus.core.crypto.spi;

import java.security.cert.X509Certificate;

/**
 * @author Ion Perpegel(perpion)
 * @since 4.0
 */
public class CertificateEntry {
    private String alias;
    private X509Certificate certificate;

    public CertificateEntry(String alias, X509Certificate certificate) {
        this.alias = alias;
        this.certificate = certificate;
    }

    public String getAlias() {
        return this.alias;
    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }
}
