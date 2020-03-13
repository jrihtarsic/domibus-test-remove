package eu.domibus.security;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.user.UserService;
import eu.domibus.core.user.ui.User;
import eu.domibus.core.user.ui.UserDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {
    public static final String CHECK_DEFAULT_PASSWORD = DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD; //NOSONAR
    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserDetailServiceImpl.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    BCryptPasswordEncoder bcryptEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userDao.loadActiveUserByUsername(userName);
        if (user == null) {
            String msg = userName + " has not been found in system";
            LOG.warn(msg);
            throw new UsernameNotFoundException(msg);
        }

        UserDetail userDetail = new UserDetail(user);
        userDetail.setDefaultPasswordUsed(isDefaultPasswordUsed(user));
        userDetail.setDaysTillExpiration(userService.getDaysTillExpiration(userName));
        return userDetail;
    }

    private boolean isDefaultPasswordUsed(final User user) {
        boolean checkDefaultPassword = Boolean.parseBoolean(domibusPropertyProvider.getProperty(CHECK_DEFAULT_PASSWORD));
        if (!checkDefaultPassword) {
            return false;
        }
        return user.hasDefaultPassword();
    }
}
