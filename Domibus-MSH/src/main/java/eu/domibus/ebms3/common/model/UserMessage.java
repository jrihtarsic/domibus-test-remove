package eu.domibus.ebms3.common.model;

import eu.domibus.core.message.fragment.MessageFragmentEntity;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

/**
 * This element has the following attributes:
 * • eb:Messaging/eb:UserMessage/@mpc: This OPTIONAL attribute contains a URI that
 * identifies the Message Partition Channel to which the message is assigned. The absence of this
 * element indicates the use of the default MPC. When the message is pulled, the value of this
 * attribute MUST indicate the MPC requested in the PullRequest message.
 * This element has the following children elements:
 * • eb:Messaging/eb:UserMessage/eb:MessageInfo: This REQUIRED element occurs once,
 * and contains data that identifies the message, and relates to other messages' identifiers.
 * • eb:Messaging/eb:UserMessage/eb:PartyInfo: This REQUIRED element occurs once,
 * and contains data about originating party and destination party.
 * • eb:Messaging/eb:UserMessage/eb:CollaborationInfo: This REQUIRED element
 * occurs once, and contains elements that facilitate collaboration between parties.
 * • eb:Messaging/eb:UserMessage/eb:MessageProperties: This OPTIONAL element
 * occurs at most once, and contains message properties that are user-specific. As parts of the
 * header such properties allow for more efficient monitoring, correlating, dispatching and validating
 * functions (even if these are out of scope of ebMS specification) which would otherwise require
 * payload access.
 * • eb:Messaging/eb:UserMessage/eb:PayloadInfo: This OPTIONAL element occurs at
 * most once, and identifies payload data associated with the message, whether included as part of
 * the message as payload document(s) contained in a Payload Container, or remote resources
 * accessible via a URL. The purpose of the PayloadInfo is (a) to make it easier to directly extract a
 * particular payload associated with this User message, (b) to allow an application to determine
 * whether it can process the payload without having to parse it.
 *
 * @author Christian Koch
 * @version 1.0
 * @since 3.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserMessage",
        propOrder = {"messageInfo", "partyInfo", "collaborationInfo", "messageProperties", "payloadInfo"})
@Entity
@Table(name = "TB_USER_MESSAGE")
public class UserMessage extends AbstractBaseEntity {

    @XmlElement(name = "MessageInfo", required = true)
    @OneToOne(cascade = CascadeType.ALL)
    protected MessageInfo messageInfo;

    @XmlElement(name = "PartyInfo", required = true)
    @Embedded
    protected PartyInfo partyInfo; //NOSONAR

    @XmlElement(name = "CollaborationInfo", required = true)
    @Embedded
    protected CollaborationInfo collaborationInfo; //NOSONAR

    @XmlElement(name = "MessageProperties")
    @Embedded
    protected MessageProperties messageProperties; //NOSONAR

    @XmlElement(name = "PayloadInfo")
    @Embedded
    protected PayloadInfo payloadInfo; //NOSONAR

    @XmlAttribute(name = "mpc")
    @XmlSchemaType(name = "anyURI")
    @Column(name = "MPC")
    protected String mpc = Ebms3Constants.DEFAULT_MPC;

    @XmlTransient
    @Column(name = "SPLIT_AND_JOIN")
    protected Boolean splitAndJoin;

    @XmlTransient
    @JoinColumn(name = "FK_MESSAGE_FRAGMENT_ID")
    @OneToOne(cascade = CascadeType.ALL)
    protected MessageFragmentEntity messageFragment;

    public MessageFragmentEntity getMessageFragment() {
        return messageFragment;
    }

    public void setMessageFragment(MessageFragmentEntity messageFragment) {
        this.messageFragment = messageFragment;
    }

    /**
     * This REQUIRED element occurs once,
     * and contains data that identifies the message, and relates to other messages' identifiers.
     *
     * @return possible object is {@link MessageInfo }
     */
    public MessageInfo getMessageInfo() {
        if (this.messageInfo == null) {
            this.messageInfo = new MessageInfo();
        }
        return this.messageInfo;
    }

    /**
     * This REQUIRED element occurs once,
     * and contains data that identifies the message, and relates to other messages' identifiers.
     *
     * @param value allowed object is {@link MessageInfo }
     */
    public void setMessageInfo(final MessageInfo value) {
        this.messageInfo = value;
    }

    /**
     * This REQUIRED element occurs once,
     * and contains data about originating party and destination party.
     *
     * @return possible object is {@link PartyInfo }
     */
    public PartyInfo getPartyInfo() {
        return this.partyInfo;
    }

    /**
     * This REQUIRED element occurs once,
     * and contains data about originating party and destination party.
     *
     * @param value allowed object is {@link PartyInfo }
     */
    public void setPartyInfo(final PartyInfo value) {
        this.partyInfo = value;
    }

    /**
     * This REQUIRED element
     * occurs once, and contains elements that facilitate collaboration between parties.
     *
     * @return possible object is {@link CollaborationInfo }
     */
    public CollaborationInfo getCollaborationInfo() {
        return this.collaborationInfo;
    }

    /**
     * This REQUIRED element
     * occurs once, and contains elements that facilitate collaboration between parties.
     *
     * @param value allowed object is {@link CollaborationInfo }
     */
    public void setCollaborationInfo(final CollaborationInfo value) {
        this.collaborationInfo = value;
    }

    /**
     * This OPTIONAL element
     * occurs at most once, and contains message properties that are user-specific. As parts of the
     * header such properties allow for more efficient monitoring, correlating, dispatching and validating
     * functions (even if these are out of scope of ebMS specification) which would otherwise require
     * payload access.
     *
     * @return possible object is {@link MessageProperties }
     */
    public MessageProperties getMessageProperties() {
        return this.messageProperties;
    }

    /**
     * This OPTIONAL element
     * occurs at most once, and contains message properties that are user-specific. As parts of the
     * header such properties allow for more efficient monitoring, correlating, dispatching and validating
     * functions (even if these are out of scope of ebMS specification) which would otherwise require
     * payload access.
     *
     * @param value allowed object is {@link MessageProperties }
     */
    public void setMessageProperties(final MessageProperties value) {
        this.messageProperties = value;
    }

    /**
     * This OPTIONAL element occurs at
     * most once, and identifies payload data associated with the message, whether included as part of
     * the message as payload document(s) contained in a Payload Container, or remote resources
     * accessible via a URL. The purpose of the PayloadInfo is (a) to make it easier to directly extract a
     * particular payload associated with this User message, (b) to allow an application to determine
     * whether it can process the payload without having to parse it.
     *
     * @return possible object is {@link PayloadInfo }
     */
    public PayloadInfo getPayloadInfo() {
        return this.payloadInfo;
    }

    /**
     * This OPTIONAL element occurs at
     * most once, and identifies payload data associated with the message, whether included as part of
     * the message as payload document(s) contained in a Payload Container, or remote resources
     * accessible via a URL. The purpose of the PayloadInfo is (a) to make it easier to directly extract a
     * particular payload associated with this User message, (b) to allow an application to determine
     * whether it can process the payload without having to parse it.
     *
     * @param value allowed object is {@link PayloadInfo }
     */
    public void setPayloadInfo(final PayloadInfo value) {
        this.payloadInfo = value;
    }

    /**
     * This OPTIONAL attribute contains a URI that
     * identifies the Message Partition Channel to which the message is assigned. The absence of this
     * element indicates the use of the default MPC. When the message is pulled, the value of this
     * attribute MUST indicate the MPC requested in the PullRequest message.
     *
     * @return possible object is {@link String }
     */
    public String getMpc() {
        return this.mpc;
    }

    /**
     * This OPTIONAL attribute contains a URI that
     * identifies the Message Partition Channel to which the message is assigned. The absence of this
     * element indicates the use of the default MPC. When the message is pulled, the value of this
     * attribute MUST indicate the MPC requested in the PullRequest message.
     *
     * @param value allowed object is {@link String }
     */
    public void setMpc(final String value) {
        this.mpc = value;
    }

    /**
     * Gives info about the payload storage approach.
     *
     * @return true if the payload has been stored on file system, false otherwise, i.e. it has been stored on DB.
     */
    public boolean isPayloadOnFileSystem() {
        for (PartInfo partInfo : getPayloadInfo().getPartInfo()) {
            if (StringUtils.isNotEmpty(partInfo.getFileName()))
                return true;
        }
        return false;
    }

    public Boolean isSplitAndJoin() {
        return BooleanUtils.toBoolean(splitAndJoin);
    }

    public void setSplitAndJoin(Boolean splitAndJoin) {
        this.splitAndJoin = splitAndJoin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserMessage that = (UserMessage) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(messageInfo, that.messageInfo)
                .append(partyInfo, that.partyInfo)
                .append(collaborationInfo, that.collaborationInfo)
                .append(messageProperties, that.messageProperties)
                .append(payloadInfo, that.payloadInfo)
                .append(mpc, that.mpc)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(messageInfo)
                .append(partyInfo)
                .append(collaborationInfo)
                .append(messageProperties)
                .append(payloadInfo)
                .append(mpc)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("messageInfo", messageInfo)
                .append("partyInfo", partyInfo)
                .append("collaborationInfo", collaborationInfo)
                .append("messageProperties", messageProperties)
                .append("payloadInfo", payloadInfo)
                .append("mpc", mpc)
                .toString();
    }

}
