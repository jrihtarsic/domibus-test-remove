package eu.domibus.common.model.logging;

import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.ebms3.common.model.MessageSubtype;
import eu.domibus.ebms3.common.model.MessageType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
public class MessageLogInfo {
    // order of the fields is important for CSV generation

    private final String messageId;

    private String fromPartyId;

    private String toPartyId;

    private final MessageStatus messageStatus;

    private final NotificationStatus notificationStatus;

    private final Date received;

    private final MSHRole mshRole;

    private final int sendAttempts;

    private final int sendAttemptsMax;

    private final Date nextAttempt;

    private String conversationId;

    private final MessageType messageType;

    private final MessageSubtype messageSubtype;

    private final Date deleted;

    private String originalSender;

    private String finalRecipient;

    private String refToMessageId;

    private final Date failed;

    private final Date restored;

    public MessageLogInfo(final String messageId,
                          final MessageStatus messageStatus,
                          final NotificationStatus notificationStatus,
                          final MSHRole mshRole,
                          final MessageType messageType,
                          final Date deleted,
                          final Date received,
                          final int sendAttempts,
                          final int sendAttemptsMax,
                          final Date nextAttempt,
                          final String conversationId,
                          final String fromPartyId,
                          final String toPartyId,
                          final String originalSender,
                          final String finalRecipient,
                          final String refToMessageId,
                          final Date failed,
                          final Date restored,
                          final MessageSubtype messageSubtype) {
        //message log information.
        this.messageId = messageId;
        this.messageStatus = messageStatus;
        this.notificationStatus = notificationStatus;
        this.mshRole = mshRole;
        this.messageType = messageType;
        this.deleted = deleted;
        this.received = received;
        this.sendAttempts = sendAttempts;
        this.sendAttemptsMax = sendAttemptsMax;
        this.nextAttempt = nextAttempt;
        //message information UserMessage/SignalMessage
        this.conversationId = conversationId;
        this.fromPartyId = fromPartyId;
        this.toPartyId = toPartyId;
        this.originalSender = originalSender;
        this.finalRecipient = finalRecipient;
        this.refToMessageId = refToMessageId;
        // rest of message log information.
        this.failed = failed;
        this.restored = restored;
        this.messageSubtype = messageSubtype;
    }



    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getFromPartyId() {
        return fromPartyId;
    }

    public void setFromPartyId(String fromPartyId) {
        this.fromPartyId = fromPartyId;
    }

    public String getToPartyId() {
        return toPartyId;
    }

    public void setToPartyId(String toPartyId) {
        this.toPartyId = toPartyId;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public void setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
    }

    public String getFinalRecipient() {
        return finalRecipient;
    }

    public void setFinalRecipient(String finalRecipient) {
        this.finalRecipient = finalRecipient;
    }

    public String getRefToMessageId() {
        return refToMessageId;
    }

    public void setRefToMessageId(String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public MSHRole getMshRole() {
        return mshRole;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Date getDeleted() {
        return deleted;
    }

    public Date getReceived() {
        return received;
    }

    public int getSendAttempts() {
        return sendAttempts;
    }

    public int getSendAttemptsMax() {
        return sendAttemptsMax;
    }

    public Date getNextAttempt() {
        return nextAttempt;
    }

    public Date getFailed() {
        return failed;
    }

    public Date getRestored() {
        return restored;
    }

    public MessageSubtype getMessageSubtype() {
        return messageSubtype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MessageLogInfo that = (MessageLogInfo) o;

        return new EqualsBuilder()
                .append(sendAttempts, that.sendAttempts)
                .append(sendAttemptsMax, that.sendAttemptsMax)
                .append(messageSubtype, that.messageSubtype)
                .append(messageId, that.messageId)
                .append(fromPartyId, that.fromPartyId)
                .append(toPartyId, that.toPartyId)
                .append(messageStatus, that.messageStatus)
                .append(notificationStatus, that.notificationStatus)
                .append(received, that.received)
                .append(mshRole, that.mshRole)
                .append(nextAttempt, that.nextAttempt)
                .append(conversationId, that.conversationId)
                .append(messageType, that.messageType)
                .append(deleted, that.deleted)
                .append(originalSender, that.originalSender)
                .append(finalRecipient, that.finalRecipient)
                .append(refToMessageId, that.refToMessageId)
                .append(failed, that.failed)
                .append(restored, that.restored)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(messageId)
                .append(fromPartyId)
                .append(toPartyId)
                .append(messageStatus)
                .append(notificationStatus)
                .append(received)
                .append(mshRole)
                .append(sendAttempts)
                .append(sendAttemptsMax)
                .append(nextAttempt)
                .append(conversationId)
                .append(messageType)
                .append(deleted)
                .append(originalSender)
                .append(finalRecipient)
                .append(refToMessageId)
                .append(failed)
                .append(restored)
                .append(messageSubtype)
                .toHashCode();
    }
}
