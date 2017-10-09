package eu.domibus.api.audit;

import java.util.Date;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public class AuditLog {

    private Long revisionId;

    private String auditTargetName;

    private String action;

    private String user;

    private Date changed;

    public AuditLog(Long revisionId, String auditTargetName, String action, String user, Date changed) {
        this.revisionId = revisionId;
        this.auditTargetName = auditTargetName;
        this.action = action;
        this.user = user;
        this.changed = changed;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public String getAuditTargetName() {
        return auditTargetName;
    }

    public String getAction() {
        return action;
    }

    public String getUser() {
        return user;
    }

    public Date getChanged() {
        return changed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditLog auditLog = (AuditLog) o;

        if (revisionId != null ? !revisionId.equals(auditLog.revisionId) : auditLog.revisionId != null) return false;
        if (!auditTargetName.equals(auditLog.auditTargetName)) return false;
        if (!action.equals(auditLog.action)) return false;
        if (!user.equals(auditLog.user)) return false;
        return changed.equals(auditLog.changed);
    }

    @Override
    public int hashCode() {
        int result = revisionId != null ? revisionId.hashCode() : 0;
        result = 31 * result + auditTargetName.hashCode();
        result = 31 * result + action.hashCode();
        result = 31 * result + user.hashCode();
        result = 31 * result + changed.hashCode();
        return result;
    }
}
