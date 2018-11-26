package eu.domibus.core.message.fragment;

import eu.domibus.ebms3.common.model.AbstractBaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@Entity
@Table(name = "TB_MESSAGE_FRAGMENT")
public class MessageFragmentEntity extends AbstractBaseEntity {

    @Column(name = "GROUP_ID")
    protected String groupId;

    @Column(name = "FRAGMENT_NUMBER")
    protected Integer fragmentNumber;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Integer getFragmentNumber() {
        return fragmentNumber;
    }

    public void setFragmentNumber(Integer fragmentNumber) {
        this.fragmentNumber = fragmentNumber;
    }
}
