package eu.domibus.core.audit.model;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author Thomas Dussart
 * @since 4.0
 *
 * Orm class in charge of representing the system audit.
 * The entity is mapped to a view (V_AUDIT).
 * V_AUDIT is a union between TB_MESSAGE_LOG table and hibernate-envers auditing tables.
 */
@Entity
@Immutable
@Table(name = "V_AUDIT")
public class Audit {

    @EmbeddedId()
    private AuditId id;

    @Column(name = "USER_NAME")
    private String user;

    public void setId(AuditId id) {
        this.id = id;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public Date getChanged() {
        return this.id.getChanged();
    }

    public String getId() {
        return id.getId();
    }

    public String getRevisionId() {
        return id.getRevisionId();
    }

    public String getAuditTargetName() {
        return id.getAuditTargetName();
    }

    public String getAction() {
        return id.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Audit audit = (Audit) o;

        if (!id.equals(audit.id)) return false;
        if (user != null ? !user.equals(audit.user) : audit.user != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
