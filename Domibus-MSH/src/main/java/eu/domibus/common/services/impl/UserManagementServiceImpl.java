package eu.domibus.common.services.impl;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainException;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.common.converters.UserConverter;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserLoginErrorReason;
import eu.domibus.common.model.security.UserRole;
import eu.domibus.common.services.UserService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static eu.domibus.common.model.security.UserLoginErrorReason.BAD_CREDENTIALS;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import eu.domibus.common.services.UserPersistenceService;

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
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected UserDomainService userDomainService;

    @Autowired
    protected UserConverter userConverter;

    @Qualifier("taskExecutor")
    @Autowired
    protected SchedulingTaskExecutor schedulingTaskExecutor; 
     
    @Autowired 
    protected UserPersistenceService userPersistenceService;

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
    @Transactional 
    public void updateUsers(List<eu.domibus.api.user.User> users) {
   
        List<eu.domibus.api.user.User> regularUsers = users.stream()
                .filter(u -> !u.getAuthorities().contains(AuthRole.ROLE_AP_ADMIN.name()))
                .collect(Collectors.toList());
        List<eu.domibus.api.user.User> superUsers = users.stream()
                .filter(u -> u.getAuthorities().contains(AuthRole.ROLE_AP_ADMIN.name()))
                .collect(Collectors.toList());

        userPersistenceService.updateUsers(regularUsers); 
         
        Future utrFuture = schedulingTaskExecutor.submit(() -> {  
            
            userPersistenceService.updateUsers(superUsers);
 
            // this block needs to called inside a transaction; for this the whole code inside the block needs to reside into a Spring bean service marked with transaction REQUIRED
        }); 
        
        try {
            utrFuture.get(3000L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new DomainException("Could not save super users", e);
        }       
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
  
}
