package eu.domibus.ebms3.common.model;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * Base type for entity
 *
 * For convenience we are using the same base entity as domibus core
 */
@XmlTransient
@MappedSuperclass
public abstract class AbstractBaseEntityNoGeneratedPk implements Serializable {


    @Id
    @XmlTransient
    @Column(name = "ID_PK")
    private long entityId;

    /**
     * @return the primary key of the entity
     */
    public long getEntityId() {
        return this.entityId;
    }


    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object other) {
        //noinspection NonFinalFieldReferenceInEquals
        return ((other != null) &&
                this.getClass().equals(other.getClass())
        );
    }
}
