package eu.domibus.common.services.impl;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomain;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserBase;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.services.PluginUserService;
import eu.domibus.security.PluginUserSecurityPolicyManager;
import eu.domibus.core.alerts.service.PluginUserAlertsServiceImpl;
import eu.domibus.core.security.AuthenticationDAO;
import eu.domibus.core.security.AuthenticationEntity;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel, Catalin Enache
 * @since 4.0
 */
@Service
public class PluginUserServiceImpl implements PluginUserService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginUserServiceImpl.class);

    @Autowired
    @Qualifier("securityAuthenticationDAO")
    private AuthenticationDAO authenticationDAO;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private DomainContextProvider domainProvider;

    @Autowired
    private PluginUserSecurityPolicyManager userSecurityPolicyManager;

    @Autowired
    PluginUserAlertsServiceImpl userAlertsService;

    @Override
    public List<AuthenticationEntity> findUsers(AuthType authType, AuthRole authRole, String originalUser, String userName, int page, int pageSize) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        return authenticationDAO.findPaged(page * pageSize, pageSize, "entityId", true, filters);
    }

    @Override
    public long countUsers(AuthType authType, AuthRole authRole, String originalUser, String userName) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        return authenticationDAO.countEntries(filters);
    }

    @Override
    @Transactional
    public void updateUsers(List<AuthenticationEntity> addedUsers, List<AuthenticationEntity> updatedUsers, List<AuthenticationEntity> removedUsers) {

        final Domain currentDomain = domainProvider.getCurrentDomain();

        checkUsers(addedUsers);

        addedUsers.forEach(u -> insertNewUser(u, currentDomain));

        updatedUsers.forEach(u -> updateUser(u));

        removedUsers.forEach(u -> deleteUser(u));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerPasswordAlerts() {
        userAlertsService.triggerPasswordExpirationEvents();
    }

    @Override
    public void reactivateSuspendedUsers() {
        userSecurityPolicyManager.reactivateSuspendedUsers();
    }

    /**
     * get all users from general schema and validate new users against existing names
     *
     * @param addedUsers
     */
    private void checkUsers(List<AuthenticationEntity> addedUsers) {
        // check duplicates with other plugin users
        for (AuthenticationEntity user : addedUsers) {
            if (!StringUtils.isEmpty(user.getUserName())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getUserName().equalsIgnoreCase(x.getUserName())))
                    throw new UserManagementException("Cannot add user " + user.getUserName() + " more than once.");
                if (!authenticationDAO.listByUser(user.getUserName()).isEmpty())
                    throw new UserManagementException("Cannot add user " + user.getUserName() + " because this name already exists.");
            }
            if (StringUtils.isNotBlank(user.getCertificateId())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getCertificateId().equalsIgnoreCase(x.getCertificateId())))
                    throw new UserManagementException("Cannot add user with certificate " + user.getCertificateId() + " more than once.");
                if (!authenticationDAO.listByCertificateId(user.getCertificateId()).isEmpty())
                    throw new UserManagementException("Cannot add user with certificate " + user.getCertificateId() + " because this certificate already exists.");
            }
        }

        // check for duplicates with other users or plugin users in multi-tenancy mode
        for (UserBase user : addedUsers) {
            // validate user not already in general schema
            userSecurityPolicyManager.validateUniqueUser(user);
        }
    }

    protected Map<String, Object> createFilterMap(AuthType authType, AuthRole authRole, String originalUser, String userName) {
        HashMap<String, Object> filters = new HashMap<>();
        if (authType != null) {
            filters.put("authType", authType.name());
        }
        if (authRole != null) {
            filters.put("authRoles", authRole.name());
        }
        filters.put("originalUser", originalUser);
        filters.put("userName", userName);
        return filters;
    }

    protected void insertNewUser(AuthenticationEntity u, Domain domain) {
        if (u.getPassword() != null) {
            u.setPassword(bcryptEncoder.encode(u.getPassword()));
        }
        authenticationDAO.create(u);

        userDomainService.setDomainForUser(u.getUniqueIdentifier(), domain.getCode());
    }

    protected void updateUser(AuthenticationEntity modified) {
        AuthenticationEntity existing = authenticationDAO.read(modified.getEntityId());

        if (StringUtils.isBlank(existing.getCertificateId())) {
            // locking policy is only applicable to Basic auth plugin users
            userSecurityPolicyManager.applyLockingPolicyOnUpdate(modified);
        }

        if (!StringUtils.isEmpty(modified.getPassword())) {
            changePassword(existing, modified.getPassword());
        }

        existing.setAuthRoles(modified.getAuthRoles());
        existing.setOriginalUser(modified.getOriginalUser());

        authenticationDAO.update(existing);
    }

    private void changePassword(AuthenticationEntity user, String newPassword) {
        userSecurityPolicyManager.changePassword(user, newPassword);
    }

    protected void deleteUser(AuthenticationEntity u) {
        AuthenticationEntity entity = authenticationDAO.read(u.getEntityId());
        authenticationDAO.delete(entity);

        userDomainService.deleteDomainForUser(u.getUniqueIdentifier());
    }
}
