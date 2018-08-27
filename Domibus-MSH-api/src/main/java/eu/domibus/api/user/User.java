package eu.domibus.api.user;

import java.util.Date;
import java.util.List;

/**
 * @author Thomas Dussart
 * @since 3.3
 */

public class User {
    private String userName;
    private String email;
    private boolean active;
    private List<String> authorities;
    private String status;
    private String password;
    private String domain;
    private boolean suspended;
    private boolean deleted;

    public User(String userName, String email, boolean active, List<String> authorities, UserState userState,
                Date suspensionDate, boolean deleted) {
        this.userName = userName;
        this.email = email;
        this.active = active;
        this.authorities = authorities;
        this.status = userState.name();
        this.password = null;
        if (suspensionDate != null) {
            this.suspended = true;
        }
        this.deleted = deleted;
    }

    public User() {
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public String getStatus() {
        return status;
    }

    public String getPassword() {
        return password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
