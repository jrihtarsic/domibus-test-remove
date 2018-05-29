package eu.domibus.core.crypto;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import org.apache.wss4j.common.crypto.CryptoBase;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.security.auth.callback.CallbackHandler;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public class Wss4JMultiDomainCryptoProvider extends CryptoBase {

    @Autowired
    MultiDomainCryptoService certificateProvider;

    @Autowired
    DomainContextProvider domainProvider;

    @Override
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getX509Certificates(currentDomain, cryptoType);
    }

    @Override
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getX509Identifier(currentDomain, cert);
    }

    @Override
    public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getPrivateKey(currentDomain, certificate, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(PublicKey publicKey, CallbackHandler callbackHandler) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getPrivateKey(currentDomain, publicKey, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getPrivateKey(currentDomain, identifier, password);
    }

    @Override
    public void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        certificateProvider.verifyTrust(currentDomain, certs, enableRevocation, subjectCertConstraints, issuerCertConstraints);
    }

    @Override
    public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        certificateProvider.verifyTrust(currentDomain, publicKey);
    }

    @Override
    public String getDefaultX509Identifier() throws WSSecurityException {
        final Domain currentDomain = domainProvider.getCurrentDomain();
        return certificateProvider.getDefaultX509Identifier(currentDomain);
    }
}
