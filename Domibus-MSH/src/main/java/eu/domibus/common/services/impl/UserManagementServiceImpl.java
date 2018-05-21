package eu.domibus.common.services.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.user.UserState;
import eu.domibus.common.converters.UserConverter;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserLoginErrorReason;
import eu.domibus.common.model.security.UserRole;
import eu.domibus.common.services.UserService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static eu.domibus.common.model.security.UserLoginErrorReason.BAD_CREDENTIALS;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@Service
public class UserManagementServiceImpl implements UserService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserManagementServiceImpl.class);

    protected static final String MAXIMUM_LOGIN_ATTEMPT = "domibus.console.login.maximum.attempt";

    protected final static String LOGIN_SUSPENSION_TIME = "domibus.console.login.suspension.time";

    private static final String DEFAULT_SUSPENSION_TIME = "3600";

    private static final String DEFAULT_LOGING_ATTEMPT = "5";

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected UserDomainService userDomainService;

    @Autowired
    protected UserConverter userConverter;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<eu.domibus.api.user.User> findUsers() {
        List<User> userEntities = userDao.listUsers();
        List<eu.domibus.api.user.User> users = userConverter.convert(userEntities);

        String domainCode = domainContextProvider.getCurrentDomainSafely().getCode();
        users.forEach(u -> u.setDomain(domainCode));

        return users;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<eu.domibus.api.user.User> findAllUsers() {
        List<eu.domibus.api.user.User> allUsers = findUsers();
        List<eu.domibus.api.user.User> superUsers = userDomainService.getSuperUsers();
        allUsers.addAll(superUsers);
        return allUsers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void saveUsers(List<eu.domibus.api.user.User> users) {
        Collection<eu.domibus.api.user.User> newUsers = filterNewUsers(users);
        LOG.debug("New users:" + newUsers.size());
        insertNewUsers(newUsers);
        Collection<eu.domibus.api.user.User> noPasswordChangedModifiedUsers = filterModifiedUserWithoutPasswordChange(users);
        LOG.debug("Modified users without password change:" + noPasswordChangedModifiedUsers.size());
        updateUserWithoutPasswordChange(noPasswordChangedModifiedUsers);
        Collection<eu.domibus.api.user.User> passwordChangedModifiedUsers = filterModifiedUserWithPasswordChange(users);
        LOG.debug("Modified users with password change:" + passwordChangedModifiedUsers.size());
        updateUserWithPasswordChange(passwordChangedModifiedUsers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<eu.domibus.api.user.UserRole> findUserRoles() {
        List<UserRole> userRolesEntities = userRoleDao.listRoles();

        List<eu.domibus.api.user.UserRole> userRoles = new ArrayList<>();
        for (UserRole userRoleEntity : userRolesEntities) {
            eu.domibus.api.user.UserRole userRole = new eu.domibus.api.user.UserRole(userRoleEntity.getName());
            userRoles.add(userRole);
        }
        return userRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUsers(List<eu.domibus.api.user.User> users) {
        // update
        Collection<eu.domibus.api.user.User> noPasswordChangedModifiedUsers = filterModifiedUserWithoutPasswordChange(users);
        LOG.debug("Modified users without password change:" + noPasswordChangedModifiedUsers.size());
        updateUserWithoutPasswordChange(noPasswordChangedModifiedUsers);
        Collection<eu.domibus.api.user.User> passwordChangedModifiedUsers = filterModifiedUserWithPasswordChange(users);
        LOG.debug("Modified users with password change:" + passwordChangedModifiedUsers.size());
        updateUserWithPasswordChange(passwordChangedModifiedUsers);

        // insertion
        Collection<eu.domibus.api.user.User> newUsers = filterNewUsers(users);
        LOG.debug("New users:" + newUsers.size());
        insertNewUsers(newUsers);

        // deletion
        List<User> usersEntities = domainConverter.convert(users, User.class);
        List<User> allUsersEntities = userDao.listUsers();
        List<User> usersEntitiesToDelete = usersToDelete(allUsersEntities, usersEntities);
        userDao.deleteAll(usersEntitiesToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoggedUserNamed() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserLoginErrorReason handleWrongAuthentication(final String userName) {
        User user = userDao.loadUserByUsername(userName);
        UserLoginErrorReason userLoginErrorReason = canApplyAccountLockingPolicy(userName, user);
        if (BAD_CREDENTIALS.equals(userLoginErrorReason)) {
            applyAccountLockingPolicy(user);
        }
        return userLoginErrorReason;
    }

    UserLoginErrorReason canApplyAccountLockingPolicy(String userName, User user) {
        if (user == null) {
            LOG.securityInfo(DomibusMessageCode.SEC_CONSOLE_LOGIN_UNKNOWN_USER, userName);
            return UserLoginErrorReason.UNKNOWN;
        }
        if (!user.isEnabled() && user.getSuspensionDate() == null) {
            LOG.securityInfo(DomibusMessageCode.SEC_CONSOLE_LOGIN_INACTIVE_USER, userName);
            return UserLoginErrorReason.INACTIVE;
        }
        if (!user.isEnabled() && user.getSuspensionDate() != null) {
            LOG.securityWarn(DomibusMessageCode.SEC_CONSOLE_LOGIN_SUSPENDED_USER, userName);
            return UserLoginErrorReason.SUSPENDED;
        }
        LOG.securityWarn(DomibusMessageCode.SEC_CONSOLE_LOGIN_BAD_CREDENTIALS, userName);
        return BAD_CREDENTIALS;
    }

    protected void applyAccountLockingPolicy(User user) {
        int maxAttemptAmount;
        try {
            maxAttemptAmount = Integer.valueOf(domibusPropertyProvider.getProperty(MAXIMUM_LOGIN_ATTEMPT, DEFAULT_LOGING_ATTEMPT));
        } catch (NumberFormatException n) {
            maxAttemptAmount = Integer.valueOf(DEFAULT_LOGING_ATTEMPT);
        }
        user.setAttemptCount(user.getAttemptCount() + 1);
        if (user.getAttemptCount() >= maxAttemptAmount) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Applying account locking policy, max number of attempt ([{}]) reached for user [{}]", maxAttemptAmount, user.getUserName());
            }
            user.setActive(false);
            user.setSuspensionDate(new Date(System.currentTimeMillis()));
            LOG.securityWarn(DomibusMessageCode.SEC_CONSOLE_LOGIN_LOCKED_USER, user.getUserName(), maxAttemptAmount);
        }
        userDao.update(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void findAndReactivateSuspendedUsers() {
        int suspensionInterval;
        try {
            suspensionInterval = Integer.valueOf(domibusPropertyProvider.getProperty(LOGIN_SUSPENSION_TIME, DEFAULT_SUSPENSION_TIME));
        } catch (NumberFormatException n) {
            suspensionInterval = Integer.valueOf(DEFAULT_SUSPENSION_TIME);
        }
        //user will not be reactivated.
        if (suspensionInterval == 0) {
            return;
        }

        Date currentTimeMinusSuspensionInterval = new Date(System.currentTimeMillis() - (suspensionInterval * 1000));
        List<User> users = userDao.getSuspendedUser(currentTimeMinusSuspensionInterval);
        for (User user : users) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Suspended user [{}] is going to be reactivated.", user.getUserName());
            }
            user.setSuspensionDate(null);
            user.setAttemptCount(0);
            user.setActive(true);
        }
        userDao.update(users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCorrectAuthentication(final String userName) {
        User user = userDao.loadActiveUserByUsername(userName);
        LOG.debug("handleCorrectAuthentication for user [{}]", userName);
        if (user.getAttemptCount() > 0) {
            LOG.debug("user [{}] has [{}] attempt ", userName, user.getAttemptCount());
            LOG.debug("reseting to 0");
            user.setAttemptCount(0);
            userDao.update(user);
        }
    }

    private List<User> usersToDelete(final List<User> masterData, final List<User> newData) {
        List<User> result = new ArrayList<>(masterData);
        result.removeAll(newData);
        return result;
    }

    private void insertNewUsers(Collection<eu.domibus.api.user.User> users) {
        for (eu.domibus.api.user.User user : users) {
            List<String> authorities = user.getAuthorities();
            User userEntity = domainConverter.convert(user, User.class);
            addRoleToUser(authorities, userEntity);
            userEntity.setPassword(bcryptEncoder.encode(userEntity.getPassword()));
            userDao.create(userEntity);
        }
    }

    private void addRoleToUser(List<String> authorities, User userEntity) {
        for (String authority : authorities) {
            UserRole userRole = userRoleDao.findByName(authority);
            userEntity.addRole(userRole);
        }
    }

    private void updateUserWithoutPasswordChange(Collection<eu.domibus.api.user.User> users) {
        for (eu.domibus.api.user.User user : users) {
            User userEntity = prepareUserForUpdate(user);
            addRoleToUser(user.getAuthorities(), userEntity);
            userDao.update(userEntity);
        }
    }

    private void updateUserWithPasswordChange(Collection<eu.domibus.api.user.User> users) {
        for (eu.domibus.api.user.User user : users) {
            User userEntity = prepareUserForUpdate(user);
            userEntity.setPassword(bcryptEncoder.encode(user.getPassword()));
            addRoleToUser(user.getAuthorities(), userEntity);
            userDao.update(userEntity);
        }
    }

    protected User prepareUserForUpdate(eu.domibus.api.user.User user) {
        User userEntity = userDao.loadUserByUsername(user.getUserName());
        if (!userEntity.getActive() && user.isActive()) {
            userEntity.setSuspensionDate(null);
            userEntity.setAttemptCount(0);
        }
        userEntity.setActive(user.isActive());
        userEntity.setEmail(user.getEmail());
        userEntity.clearRoles();
        return userEntity;
    }

    private Collection<eu.domibus.api.user.User> filterNewUsers(List<eu.domibus.api.user.User> users) {
        return Collections2.filter(users, new Predicate<eu.domibus.api.user.User>() {
            @Override
            public boolean apply(eu.domibus.api.user.User user) {
                return UserState.NEW.name().equals(user.getStatus());
            }
        });
    }

    private Collection<eu.domibus.api.user.User> filterModifiedUserWithoutPasswordChange(List<eu.domibus.api.user.User> users) {
        return Collections2.filter(users, new Predicate<eu.domibus.api.user.User>() {
            @Override
            public boolean apply(eu.domibus.api.user.User user) {
                return UserState.UPDATED.name().equals(user.getStatus()) && user.getPassword() == null;
            }
        });
    }

    private Collection<eu.domibus.api.user.User> filterModifiedUserWithPasswordChange(List<eu.domibus.api.user.User> users) {
        return Collections2.filter(users, new Predicate<eu.domibus.api.user.User>() {
            @Override
            public boolean apply(eu.domibus.api.user.User user) {
                return UserState.UPDATED.name().equals(user.getStatus()) && user.getPassword() != null && !user.getPassword().isEmpty();
            }
        });
    }

}
