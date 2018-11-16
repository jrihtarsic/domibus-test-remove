package eu.domibus.web.rest;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainException;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserDetail;
import eu.domibus.common.services.UserPersistenceService;
import eu.domibus.common.util.WarningUtil;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.security.AuthenticationService;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.DomainRO;
import eu.domibus.web.rest.ro.LoginRO;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@RunWith(JMockit.class)
public class AuthenticationResourceTest {

    @Tested
    AuthenticationResource authenticationResource;

    @Injectable
    AuthenticationService authenticationService;

    @Injectable
    UserDomainDao userDomainDao;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    UserDomainService userDomainService;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomainCoreConverter domainCoreConverter;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    protected UserPersistenceService userPersistenceService;

    @Mocked
    Logger LOG;

    @Test
    public void testWarningWhenDefaultPasswordUsed(@Mocked WarningUtil warningUtil) throws Exception {
        User user = new User("user", "user");
        user.setDefaultPassword(true);
        LoginRO loginRO = new LoginRO();
        loginRO.setUsername("user");
        loginRO.setPassword("user");
        final UserDetail userDetail = new UserDetail(user);
        new Expectations() {{
            userDomainService.getDomainForUser(loginRO.getUsername());
            result = DomainService.DEFAULT_DOMAIN.getCode();

            authenticationService.authenticate("user", "user", DomainService.DEFAULT_DOMAIN.getCode());
            result = userDetail;
        }};
        authenticationResource.authenticate(loginRO, new MockHttpServletResponse());
        new Verifications() {{
            String message;
            WarningUtil.warnOutput(message = withCapture());
            assertEquals("user is using default password.", message);
        }};
    }

    @Test
    public void testGetCurrentDomain(@Mocked final LoggerFactory loggerFactory) {
        // Given
        final DomainRO domainRO = new DomainRO();
        domainRO.setCode(DomainService.DEFAULT_DOMAIN.getCode());
        domainRO.setName(DomainService.DEFAULT_DOMAIN.getName());

        new Expectations(authenticationResource) {{
            domainContextProvider.getCurrentDomainSafely();
            result = DomainService.DEFAULT_DOMAIN;

            domainCoreConverter.convert(DomainService.DEFAULT_DOMAIN, DomainRO.class);
            result = domainRO;
        }};

        // When
        final DomainRO result = authenticationResource.getCurrentDomain();

        // Then
        Assert.assertEquals(domainRO, result);
    }

    @Test(expected = DomainException.class)
    public void testExceptionInSetCurrentDomain(@Mocked final LoggerFactory loggerFactory) {
        // Given
        new Expectations(authenticationResource) {{
            authenticationService.changeDomain("");
            result = new DomainException("");
        }};
        // When
        authenticationResource.setCurrentDomain("");
        // Then
        // expect DomainException
    }
}