package eu.domibus.common.model.common;

import eu.domibus.common.listener.CustomRevisionEntityListener;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Own implementation of hibernate-envers Revision entity, in order to store the user and the modification type.
 *
 * @author Thomas Dussart
 * @since 4.0
 */
@Entity
@Table(name = "TB_REV_INFO")
@RevisionEntity(CustomRevisionEntityListener.class)
public class RevisionLog {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(RevisionLog.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;
    /**
     * User involve in this modification
     */
    @Column(name = "USER_NAME")
    private String userName;
    /**
     * Date of the modification.
     */
    @Column(name = "REVISION_DATE")
    private Date revisionDate;
    /**
     * Different entities can be modified during the same transaction update/create/delete.
     * This field reflect the list of entities.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "REV")
    @Fetch(FetchMode.JOIN)
    private Set<EnversAudit> revisionTypes = new HashSet<>();

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getRevisionDate() {
        return revisionDate;
    }

    public void setRevisionDate(Date revisionDate) {
        this.revisionDate = revisionDate;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof RevisionLog)) {
            return false;
        } else {
            RevisionLog that = (RevisionLog)o;
            return this.id == that.id && this.timestamp == that.timestamp;
        }
    }

    public int hashCode() {
        int result = this.id;
        result = 31 * result + (int)(this.timestamp ^ this.timestamp >>> 32);
        return result;
    }

    /**
     * Hibernate is going to send notifications to this revision class each time an update occurs on an entity or its sub-hierarchy,
     * but in the audit system we only want to keep track of the changes at a highber level. If wee need more info it is possible to go into envers audit tables.
     *
     * @param entityId the entity id
     * @param entityName the entity name
     * @param groupName the group name
     * @param modificationType the modification type
     * @param auditOrder the audit order
     */
    public void addEntityAudit(final String entityId, final String entityName, final String groupName, final ModificationType modificationType, final int auditOrder) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding envers audit " + entityId + " entity name " + entityName + " groupName " + groupName + " modificationType " + modificationType + " auditOrder " + auditOrder);
        }
        EnversAudit enversAudit = new EnversAudit();
        enversAudit.setGroupName(groupName);
        enversAudit.setEntityName(entityName);
        enversAudit.setModificationType(modificationType);
        enversAudit.setId(entityId);
        enversAudit.setAuditOrder(auditOrder);
        revisionTypes.add(enversAudit);


        //remove every entry with higher audit order. (Eg: when you update a PMODE, you also update and/or add a a PARTY , but in the audit system you just want that an action has been made on the pmode).
        Optional<Integer> min = this.revisionTypes.stream().map(EnversAudit::getAuditOrder).min(Integer::compareTo);
        min.ifPresent(integer -> revisionTypes.removeIf(r -> r.getAuditOrder() > integer));

        //If an entity is added and then modified in the same revision, envers will trigger ADD then MOD. In ower audit system we only
        //want to see that an entity has been added eg:User/ADD. So ADD wil have a lower order that MOD, and mod will not be saved in the central audit table.
        //Of course the full action perfomed on the entity can be retrieved in the entity audit specific audit table.

        //group entities version on their ids.
        Map<String, List<EnversAudit>> collect = revisionTypes.stream()
                .collect(Collectors.groupingBy(EnversAudit::getId));
        //Iterate over a collection of same entity ids.
        for (List<EnversAudit> enversAudits : collect.values()) {
            //retrieve the entity wih min modification type.
            min = enversAudits.stream().map(EnversAudit::getModificationType).map(ModificationType::getOrder).min(Integer::compareTo);
            //Remove the entity from the temporary list and Keep only the one with higher order, the one that we want to eliminate.
            min.ifPresent(integer -> enversAudits.removeIf(r -> r.getModificationType().getOrder() == integer));
            //substract temporary list from global entity list.
            revisionTypes.removeAll(enversAudits);
        }
    }

    public Set<EnversAudit> getRevisionTypes() {
        return Collections.unmodifiableSet(revisionTypes);
    }
}
