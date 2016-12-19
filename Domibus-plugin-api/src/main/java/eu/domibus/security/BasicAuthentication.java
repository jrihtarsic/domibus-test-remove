package eu.domibus.security;

import eu.domibus.common.AuthRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasicAuthentication implements Authentication {

    private static final Log LOG = LogFactory.getLog(BasicAuthentication.class);

	private boolean authenticated;
	private String user = null;
	private String password = null;
    private String originalUser = null;
    private Collection<GrantedAuthority> authorityList = null;


	public BasicAuthentication(String user, String password){
		this.user = user;
		this.password = password;
	}

	public BasicAuthentication(AuthRole authRole) {
		this.user = null;
		this.password = null;
		List<GrantedAuthority> roles = new ArrayList<>();
		roles.add(new SimpleGrantedAuthority(authRole.name()));
		this.authorityList = roles;
	}


    @Override
    public Object getPrincipal() { return originalUser; }

    @Override
    public Object getCredentials() {
        return password;
    }

    @Override
    public Object getDetails() {
        return user != null ? user : "" + ":" + password != null ? password : "";
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated)
            throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
	public String getName() { return user;	}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityList;
    }

    public void setAuthorityList(Collection<GrantedAuthority> authorityList) {
        this.authorityList = authorityList;
    }

    public String getOriginalUser() {
        return originalUser;
    }

    public void setOriginalUser(String originalUser) {
        this.originalUser = originalUser;
    }
}
