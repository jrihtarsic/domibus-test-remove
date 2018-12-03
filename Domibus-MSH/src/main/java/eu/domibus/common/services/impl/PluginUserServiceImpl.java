package eu.domibus.common.services.impl;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.services.PluginUserService;
import eu.domibus.common.validators.PluginUserPasswordManager;
import eu.domibus.core.alerts.service.PluginUserAlertsServiceImpl;
import eu.domibus.core.security.AuthenticationDAO;
import eu.domibus.core.security.AuthenticationEntity;
import eu.domibus.core.security.PluginUserPasswordHistoryDao;
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

/**
 * @author Ion Perpegel, Catalin Enache
 * @since 4.0
 */
@Service
public class PluginUserServiceImpl implements PluginUserService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginUserService.class);

    @Autowired
    @Qualifier("securityAuthenticationDAO")
    private AuthenticationDAO securityAuthenticationDAO;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private DomainContextProvider domainProvider;

    @Autowired
    private PluginUserPasswordManager passwordManager;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private PluginUserPasswordHistoryDao userPasswordHistoryDao;

    @Autowired
    PluginUserAlertsServiceImpl userAlertsService;

    @Override
    public List<AuthenticationEntity> findUsers(AuthType authType, AuthRole authRole, String originalUser, String userName, int page, int pageSize) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        return securityAuthenticationDAO.findPaged(page * pageSize, pageSize, "entityId", true, filters);
    }

    @Override
    public long countUsers(AuthType authType, AuthRole authRole, String originalUser, String userName) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        return securityAuthenticationDAO.countEntries(filters);
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
    public void sendAlerts() {
        userAlertsService.sendAlerts();
    }

    /**
     * get all users from general schema and validate new users against existing names
     *
     * @param addedUsers
     */
    private void checkUsers(List<AuthenticationEntity> addedUsers) {
        // check duplicates with other plugin users
        for (AuthenticationEntity user : addedUsers) {
            if (!StringUtils.isEmpty(user.getUsername())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getUsername().equalsIgnoreCase(x.getUsername())))
                    throw new UserManagementException("Cannot add user " + user.getUsername() + " more than once.");
                if (!securityAuthenticationDAO.listByUser(user.getUsername()).isEmpty())
                    throw new UserManagementException("Cannot add user " + user.getUsername() + " because this name already exists.");
            }
            if (!StringUtils.isEmpty(user.getCertificateId())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getCertificateId().equalsIgnoreCase(x.getCertificateId())))
                    throw new UserManagementException("Cannot add user with certificate " + user.getCertificateId() + " more than once.");
                if (!securityAuthenticationDAO.listByCertificateId(user.getCertificateId()).isEmpty())
                    throw new UserManagementException("Cannot add user with certificate " + user.getCertificateId() + " because this certificate already exists.");
            }
        }

        // check for duplicates with other users or plugin users in multi-tenancy mode
        List<String> allUserNames = userDomainService.getAllUserNames();
        for (AuthenticationEntity user : addedUsers) {
            if (allUserNames.stream().anyMatch(name -> name.equalsIgnoreCase(user.getUsername())))
                throw new UserManagementException("Cannot add user " + user.getUsername() + " because this name already exists.");
        }
    }

    private Map<String, Object> createFilterMap(
            AuthType authType,
            AuthRole authRole,
            String originalUser,
            String userName) {
        HashMap<String, Object> filters = new HashMap<>();
        if (authType != null) {
            filters.put("authType", authType.name());
        }
        if (authRole != null) {
            filters.put("authRoles", authRole.name());
        }
        filters.put("originalUser", originalUser);
        filters.put("username", userName);
        return filters;
    }

    private void insertNewUser(AuthenticationEntity u, Domain domain) {
        if (u.getPassword() != null) {
            u.setPassword(bcryptEncoder.encode(u.getPassword()));
        }
        securityAuthenticationDAO.create(u);

        String userIdentifier = u.getCertificateId() != null ? u.getCertificateId() : u.getUsername();
        userDomainService.setDomainForUser(userIdentifier, domain.getCode());
    }

    private void updateUser(AuthenticationEntity modified) {
        AuthenticationEntity existing = securityAuthenticationDAO.read(modified.getEntityId());
        if (modified.getPassword() != null) {
            changePassword(existing, modified.getPassword());
        }
        existing.setAuthRoles(modified.getAuthRoles());
        existing.setOriginalUser(modified.getOriginalUser());
        securityAuthenticationDAO.update(existing);
    }

    //TODO: try to merge this code with the similar one found in UserPersistenceServiceImpl
    private void changePassword(AuthenticationEntity user, String newPassword) {
        passwordManager.changePassword(user, newPassword);
    }

    private void deleteUser(AuthenticationEntity u) {
        AuthenticationEntity entity = securityAuthenticationDAO.read(u.getEntityId());
        securityAuthenticationDAO.delete(entity);

        String userIdentifier = u.getCertificateId() != null ? u.getCertificateId() : u.getUsername();
        userDomainService.deleteDomainForUser(userIdentifier);
    }
}
