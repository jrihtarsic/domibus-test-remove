package eu.domibus.security;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.dao.security.ConsoleUserPasswordHistoryDao;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.dao.security.UserDaoBase;
import eu.domibus.common.dao.security.UserPasswordHistoryDao;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserEntityBase;
import eu.domibus.core.alerts.service.ConsoleUserAlertsServiceImpl;
import eu.domibus.core.alerts.service.UserAlertsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

/**
 * @author Ion Perpegel
 * @since 4.1
 * Template method pattern derived class responsible for particularities of console users
 */

@Service
public class ConsoleUserSecurityPolicyManager extends UserSecurityPolicyManager<User> {
    static final String WARNING_DAYS_BEFORE_EXPIRATION = DOMIBUS_PASSWORD_POLICY_WARNING_BEFORE_EXPIRATION;

    static final String PASSWORD_COMPLEXITY_PATTERN = DOMIBUS_PASSWORD_POLICY_PATTERN; //NOSONAR
    static final String PASSWORD_HISTORY_POLICY = DOMIBUS_PASSWORD_POLICY_DONT_REUSE_LAST; //NOSONAR

    static final String MAXIMUM_PASSWORD_AGE = DOMIBUS_PASSWORD_POLICY_EXPIRATION; //NOSONAR
    static final String MAXIMUM_DEFAULT_PASSWORD_AGE = DOMIBUS_PASSWORD_POLICY_DEFAULT_PASSWORD_EXPIRATION; //NOSONAR

    protected static final String MAXIMUM_LOGIN_ATTEMPT = DOMIBUS_CONSOLE_LOGIN_MAXIMUM_ATTEMPT;

    protected static final String LOGIN_SUSPENSION_TIME = DOMIBUS_CONSOLE_LOGIN_SUSPENSION_TIME;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected UserDao userDao;

    @Autowired
    private ConsoleUserPasswordHistoryDao userPasswordHistoryDao;

    @Autowired
    private ConsoleUserAlertsServiceImpl userAlertsService;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Override
    protected String getPasswordComplexityPatternProperty() {
        return PASSWORD_COMPLEXITY_PATTERN;
    }

    @Override
    public String getPasswordHistoryPolicyProperty() {
        return PASSWORD_HISTORY_POLICY;
    }

    @Override
    public String getMaximumDefaultPasswordAgeProperty() {
        return MAXIMUM_DEFAULT_PASSWORD_AGE;
    }

    @Override
    protected String getMaximumPasswordAgeProperty() {
        return MAXIMUM_PASSWORD_AGE;
    }

    @Override
    public String getWarningDaysBeforeExpirationProperty() {
        return WARNING_DAYS_BEFORE_EXPIRATION;
    }

    @Override
    protected UserPasswordHistoryDao getUserHistoryDao() {
        return userPasswordHistoryDao;
    }

    @Override
    protected UserDaoBase getUserDao() {
        return userDao;
    }

    @Override
    protected int getMaxAttemptAmount(UserEntityBase user) {
        final Domain domain = getCurrentOrDefaultDomainForUser((User) user);
        return domibusPropertyProvider.getIntegerDomainProperty(domain, MAXIMUM_LOGIN_ATTEMPT);
    }

    @Override
    protected UserAlertsService getUserAlertsService() {
        return userAlertsService;
    }

    @Override
    protected int getSuspensionInterval() {
        Domain domain = domainContextProvider.getCurrentDomainSafely();

        int suspensionInterval;
        if (domain == null) { //it is called for super-users so we read from default domain
            suspensionInterval = domibusPropertyProvider.getIntegerProperty(LOGIN_SUSPENSION_TIME);
        } else { //for normal users the domain is set as current Domain
            suspensionInterval = domibusPropertyProvider.getIntegerDomainProperty(LOGIN_SUSPENSION_TIME);
        }
        return suspensionInterval;
    }

    @Override
    protected UserEntityBase.Type getUserType() {
        return UserEntityBase.Type.CONSOLE;
    }

    private Domain getCurrentOrDefaultDomainForUser(User user) {
        String domainCode;
        boolean isSuperAdmin = user.isSuperAdmin();
        if (isSuperAdmin) {
            domainCode = DomainService.DEFAULT_DOMAIN.getCode();
        } else {
            domainCode = userDomainService.getDomainForUser(user.getUserName());
        }
        return domainService.getDomain(domainCode);
    }

}
