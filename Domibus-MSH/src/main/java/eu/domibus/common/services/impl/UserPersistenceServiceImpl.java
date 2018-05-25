/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.domibus.common.services.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainException;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.user.UserState;
import eu.domibus.common.converters.UserConverter;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserRole;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.transaction.TransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import eu.domibus.common.services.UserPersistenceService;

/**
 *
 * @author Pion
 */
@Service
public class UserPersistenceServiceImpl implements UserPersistenceService {
     
    
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserPersistenceServiceImpl.class);
    
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

    @Qualifier("taskExecutor")
    @Autowired
    protected SchedulingTaskExecutor schedulingTaskExecutor;
    
    @Autowired
    protected TransactionManager transactionManager;
     
    @Override 
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateUsers(List<eu.domibus.api.user.User> users) {
  
        final Domain currentDomainSafely = domainContextProvider.getCurrentDomainSafely();
        LOG.info("Current domain " + currentDomainSafely);

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

        /*
        // deletion - TODO: delete logically
        List<User> usersEntities = domainConverter.convert(users, User.class);
        List<User> allUsersEntities = userDao.listUsers();
        List<User> usersEntitiesToDelete = usersToDelete(allUsersEntities, usersEntities);
        userDao.deleteAll(usersEntitiesToDelete);
        */
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

    private List<User> usersToDelete(final List<User> masterData, final List<User> newData) {
        List<User> result = new ArrayList<>(masterData);
        result.removeAll(newData);
        return result;
    }

    private void insertNewUsers(Collection<eu.domibus.api.user.User> users) {
        for (eu.domibus.api.user.User user : users) {
            List<String> authorities = user.getAuthorities();
            User userEntity = domainConverter.convert(user, User.class);
            userEntity.setPassword(bcryptEncoder.encode(userEntity.getPassword())); 
            addRoleToUser(authorities, userEntity);
            userDao.create(userEntity);

            if (user.getAuthorities().contains(AuthRole.ROLE_AP_ADMIN.name())) { 
                userDomainService.setPreferredDomainForUser(user.getUserName(), user.getDomain());
            } else { 
                userDomainService.setDomainForUser(user.getUserName(), user.getDomain());
            }
        }
    }

    private void addRoleToUser(List<String> authorities, User userEntity) {
        for (String authority : authorities) {
            UserRole userRole = userRoleDao.findByName(authority);
            userEntity.addRole(userRole);
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
