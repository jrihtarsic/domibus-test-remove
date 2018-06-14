package eu.domibus.core.security;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

@Component(value = "domibusAuthenticationService")
public class AuthenticationDefaultService implements AuthenticationService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AuthenticationDefaultService.class);

    protected static final String BASIC_HEADER_KEY = "Authorization";
    protected static final String CLIENT_CERT_ATTRIBUTE_KEY = "javax.servlet.request.X509Certificate";
    protected static final String CLIENT_CERT_HEADER_KEY = "Client-Cert";


    @Autowired
    protected AuthUtils authUtils;

    @Autowired
    @Qualifier("securityCustomAuthenticationProvider")
    private AuthenticationProvider authenticationProvider;

    @Autowired
    protected UserDomainService userDomainService;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Override
    public void authenticate(HttpServletRequest httpRequest) throws AuthenticationException {
        LOG.debug("Authenticating for " + httpRequest.getRequestURI());

        /* id domibus allows unsecure login, do not authenticate anymore, just go on */
        if (authUtils.isUnsecureLoginAllowed()) {
            LOG.securityInfo(DomibusMessageCode.SEC_UNSECURED_LOGIN_ALLOWED);
            return;
        }

        final Object certificateAttribute = httpRequest.getAttribute(CLIENT_CERT_ATTRIBUTE_KEY);
        final String certHeaderValue = httpRequest.getHeader(CLIENT_CERT_HEADER_KEY);
        final String basicHeaderValue = httpRequest.getHeader(BASIC_HEADER_KEY);

        if (basicHeaderValue != null) {
            LOG.debug("Basic authentication header found: " + basicHeaderValue);
        }
        if (certificateAttribute != null) {
            LOG.debug("CertificateAttribute found: " + certificateAttribute.getClass());
        }
        if (certHeaderValue != null) {
            LOG.debug("Client certificate in header found: " + certHeaderValue);
        }

        if (basicHeaderValue != null && basicHeaderValue.startsWith("Basic")) {
            LOG.securityInfo(DomibusMessageCode.SEC_BASIC_AUTHENTICATION_USE);

            LOG.debug("Basic authentication: " + Base64.decode(basicHeaderValue.substring("Basic ".length())));
            String basicAuthCredentials = new String(Base64.decode(basicHeaderValue.substring("Basic ".length())));
            int index = basicAuthCredentials.indexOf(":");
            String user = basicAuthCredentials.substring(0, index);
            final String domainForUser = userDomainService.getDomainForUser(user);
            domainContextProvider.setCurrentDomain(domainForUser);
            String password = basicAuthCredentials.substring(index + 1);
            BasicAuthentication authentication = new BasicAuthentication(user, password);
            authenticate(authentication, httpRequest);
        } else if ("https".equalsIgnoreCase(httpRequest.getScheme())) {
            if (certificateAttribute == null) {
                throw new AuthenticationException("No client certificate present in the request");
            }
            if (!(certificateAttribute instanceof X509Certificate[])) {
                throw new AuthenticationException("Request value is not of type X509Certificate[] but of " + certificateAttribute.getClass());
            }
            LOG.securityInfo(DomibusMessageCode.SEC_X509CERTIFICATE_AUTHENTICATION_USE);
            final X509Certificate[] certificates = (X509Certificate[]) certificateAttribute;
            Authentication authentication = new X509CertificateAuthentication(certificates);
            String user = ((X509CertificateAuthentication)authentication).getCertificateId();
            final String domainForUser = userDomainService.getDomainForUser(user);
            domainContextProvider.setCurrentDomain(domainForUser);
            authenticate(authentication, httpRequest);
        } else if ("http".equalsIgnoreCase(httpRequest.getScheme())) {
            if (certHeaderValue == null) {
                throw new AuthenticationException(DomibusCoreErrorCode.DOM_002, "There is no valid authentication in this request and unsecure login is not allowed.");
            }
            LOG.securityInfo(DomibusMessageCode.SEC_BLUE_COAT_AUTHENTICATION_USE);
            Authentication authentication = new BlueCoatClientCertificateAuthentication(certHeaderValue);
            String user = ((BlueCoatClientCertificateAuthentication)authentication).getCertificateId();
            final String domainForUser = userDomainService.getDomainForUser(user);
            domainContextProvider.setCurrentDomain(domainForUser);
            authenticate(authentication, httpRequest);
        } else {
            throw new AuthenticationException("There is no valid authentication in this request and unsecure login is not allowed.");
        }

    }

    private void authenticate(Authentication authentication, HttpServletRequest httpRequest) throws AuthenticationException {
        LOG.securityInfo(DomibusMessageCode.SEC_CONNECTION_ATTEMPT, httpRequest.getRemoteHost(), httpRequest.getRequestURL());
        Authentication authenticationResult;
        try {
            authenticationResult = authenticationProvider.authenticate(authentication);
        } catch (org.springframework.security.core.AuthenticationException exc) {
            throw new AuthenticationException("Error while authenticating " + authentication.getName(), exc);
        }

        if (authenticationResult.isAuthenticated()) {
            LOG.securityInfo(DomibusMessageCode.SEC_AUTHORIZED_ACCESS, httpRequest.getRemoteHost(), httpRequest.getRequestURL(), authenticationResult.getAuthorities());
            LOG.debug("Request authenticated. Storing the authentication result in the security context");
            LOG.debug("Authentication result: " + authenticationResult);
            SecurityContextHolder.getContext().setAuthentication(authenticationResult);
            LOG.putMDC(DomibusLogger.MDC_USER, authenticationResult.getName());
        } else {
            LOG.securityInfo(DomibusMessageCode.SEC_UNAUTHORIZED_ACCESS, httpRequest.getRemoteHost(), httpRequest.getRequestURL());
            throw new AuthenticationException("The certificate is not valid or is not present or the basic authentication credentials are invalid");
        }
    }
}
