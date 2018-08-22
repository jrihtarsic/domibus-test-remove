package eu.domibus.web.filter;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.common.model.security.UserDetail;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public class SetDomainFilter extends GenericFilterBean {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(SetDomainFilter.class);

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && (authentication.getPrincipal() instanceof UserDetail)) {
            UserDetail securityUser = (UserDetail) authentication.getPrincipal();
            domainContextProvider.setCurrentDomain(getCurrentDomain(securityUser));

        }
        chain.doFilter(request, response);
    }

    protected String getCurrentDomain(UserDetail securityUser) {
        final boolean multiTenantAware = domibusConfigurationService.isMultiTenantAware();
        String result = DomainService.DEFAULT_DOMAIN.getCode();
        if (multiTenantAware) {
            result = securityUser.getDomain();
        }
        return result;
    }
}
