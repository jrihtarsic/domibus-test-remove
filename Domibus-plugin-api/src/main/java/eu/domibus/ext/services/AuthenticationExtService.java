package eu.domibus.ext.services;

import eu.domibus.ext.exceptions.AuthenticationExtException;

import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for operations related to the plugin authentication
 *
 * @author Cosmin Baciu
 * @since 4.0
 */
public interface AuthenticationExtService {

    /**
     * Authenticates the caller using one of the following authentication methods in this specific order: basic authentication, https or blue coat
     *
     * @param httpRequest the HttpServletRequest request
     * @throws AuthenticationExtException in case an error occurs while authenticating the caller
     */
    void authenticate(HttpServletRequest httpRequest) throws AuthenticationExtException;

    /**
     * Authenticates the caller using basic authentication method
     *
     * @param username The username used for authentication
     * @param password The user password
     * @throws AuthenticationExtException
     */
    void basicAuthenticate(String username, String password) throws AuthenticationExtException;

    /**
     * Checks whether unsecure loggin is allowed
     *
     * @return true in case unsecure loggin is allowed
     */
    boolean isUnsecureLoginAllowed();

}
