package eu.domibus.core.security;

import eu.domibus.core.crypto.spi.AuthorizationServiceSpi;
import eu.domibus.core.crypto.spi.model.PullRequestMapping;
import eu.domibus.core.crypto.spi.model.UserMessageMapping;
import eu.domibus.ext.domain.PullRequestDTO;
import eu.domibus.ext.domain.UserMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Component
public class DefaultAuthorizationServiceSpiImpl implements AuthorizationServiceSpi {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorizationServiceSpiImpl.class);

    protected static final String DEFAULT_IAM_AUTHORIZATION_IDENTIFIER = "DEFAULT_IAM_AUTHORIZATION_SPI";

    @Override
    public boolean authorize(X509Certificate[] certs, UserMessageDTO userMessage, Map<UserMessageMapping, String> messageMapping) {
        LOG.info("Default authorization not implemented.");
        return true;
    }

    @Override
    public boolean authorize(X509Certificate[] certs, PullRequestDTO pullRequestDTO, Map<PullRequestMapping, String> pullRequestMapping) {
        return true;
    }

    @Override
    public String getIdentifier() {
        return DEFAULT_IAM_AUTHORIZATION_IDENTIFIER;
    }

}
