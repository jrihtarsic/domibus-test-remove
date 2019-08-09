package eu.domibus.pki;

import com.google.common.collect.Lists;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;


/**
 * @author idragusa
 * @since 4.1.1
 * <p>
 * X509TrustManager with the custom truststore configured in Domibus in addition to the cacerts </p>
 */
@Service
public class DomibusX509TrustManager implements X509TrustManager {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusX509TrustManager.class);

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected DomainContextProvider domainProvider;

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509TrustManager defaultTm;
        try {
            defaultTm = getX509TrustManager(true);
        } catch (NoSuchAlgorithmException | KeyStoreException exc) {
            LOG.warn("Could not load default certificates.");
            return null;
        }
        return defaultTm.getAcceptedIssuers();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException {
        X509TrustManager defaultTm;
        X509TrustManager domibusTm;
        try {
            defaultTm = getX509TrustManager(true);
            domibusTm = getX509TrustManager(false);
        } catch (NoSuchAlgorithmException | KeyStoreException exc) {
            LOG.warn("Could not load default/custom certificates.");
            throw new CertificateException("Could not load default/custom certificates.", exc);
        }

        try {
            domibusTm.checkServerTrusted(chain, authType);
        } catch (CertificateException e) {
            // This will throw another CertificateException if this fails too.
            defaultTm.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException {
        X509TrustManager defaultTm;
        try {
            defaultTm = getX509TrustManager(true);
        } catch (NoSuchAlgorithmException | KeyStoreException exc) {
            LOG.warn("Could not load default certificates.");
            throw new CertificateException("Could not load default certificates.", exc);
        }
        defaultTm.checkClientTrusted(chain, authType);
    }

    protected X509TrustManager getX509TrustManager(boolean isDefaultTrust) throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore trustStore = null;
        X509TrustManager trustManager = null;
        if (!isDefaultTrust) {
            LOG.debug("Getting custom certificates [{}]", trustStore);
            trustStore = multiDomainCertificateProvider.getTrustStore(domainProvider.getCurrentDomain());
        } else {
            LOG.debug("Getting default certificates");
        }

        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Using null here initialises the TMF with the default trust store (cacerts).
            tmf.init(trustStore);

            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;
                    break;
                }
            }
        } catch (NoSuchAlgorithmException | KeyStoreException exc) {
            LOG.warn("Could not load trustManager for ssl exchange. ", exc);
            throw exc;
        }

        return trustManager;
    }

    public KeyStore instantiateTrustStoreFromTrustManager() throws KeyStoreException, NoSuchAlgorithmException {
        X509TrustManager cacertTrustManager = getX509TrustManager(true);
        X509TrustManager customTrustManager = getX509TrustManager(false);
        List<X509Certificate> cacertCertificates = Lists.newArrayList(cacertTrustManager.getAcceptedIssuers());
        List<X509Certificate> customTlsCertificates = Lists.newArrayList(customTrustManager.getAcceptedIssuers());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cacert certificate:");
            for (X509Certificate cacX509Certificate : cacertCertificates) {
                LOG.debug("Serical:[{}], subject:[{}]", cacX509Certificate.getSerialNumber(), cacX509Certificate.getSubjectDN());
            }
            LOG.debug("Custom TLS certificates");
            for (X509Certificate customTlsCertificate : customTlsCertificates) {
                LOG.debug("Serical:[{}], subject:[{}]", customTlsCertificate.getSerialNumber(), customTlsCertificate.getSubjectDN());
            }
        }
        return null;
    }
}
