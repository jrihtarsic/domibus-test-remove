package eu.domibus.common.services;

import eu.domibus.api.user.User;
import eu.domibus.common.model.security.UserLoginErrorReason;

import java.util.List;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
public interface UserService{

    /**
     * @return the list of system users.
     */
    List<eu.domibus.api.user.User> findUsers();

    /**
     * @return the list of users, including super users.
     */
    List<eu.domibus.api.user.User> findAllUsers();

    /**
     * create or update users of the system (edited in the user management gui console).
     * @param users to create of update.
     */
    void saveUsers(List<eu.domibus.api.user.User> users);

    /**
     * get all user roles
     */
    List<eu.domibus.api.user.UserRole> findUserRoles();

    /**
     * update users
     */
    void updateUsers(List<User> users);

    /**
     * Handle the account lockout policy.
     * Will log login attempt to the security log and inactivate user after certain amount of login attempt.
     *
     * @param userName the user loggin string
     * @return the reason of the login error.
     */
    UserLoginErrorReason handleWrongAuthentication(final String userName);

    /**
     * Search for all users that have been suspended (due to multiple unsuccessful login attempts)
     * and verify if the suspension date is smaller then current time - interval period defined in property file.
     * If some user are found they will be reactivated.
     */
    void findAndReactivateSuspendedUsers();

    /**
     * Get currently logged user name.
     *
     * @return the userName
     */
    String getLoggedUserNamed();

    /**
     * Verify if user add some incorrect login attempt and reset the attempt counter.
     * @param username the userName
     */
    void handleCorrectAuthentication(String username);
}
