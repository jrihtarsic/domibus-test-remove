package eu.domibus.common.model.logging;

import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.ebms3.common.model.MessageType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;
import java.util.Objects;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
public class MessageLogInfo {

    private String conversationId;

    private String fromPartyId;

    private String toPartyId;

    private String originalSender;

    private String finalRecipient;

    private String refToMessageId;

    private final String messageId;

    private final MessageStatus messageStatus;

    private final NotificationStatus notificationStatus;

    private final MSHRole mshRole;

    private final MessageType messageType;

    private final Date deleted;

    private final Date received;

    private final int sendAttempts;

    private final int sendAttemptsMax;

    private final Date nextAttempt;

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
                          final Date restored) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MessageLogInfo that = (MessageLogInfo) o;

        return new EqualsBuilder()
                .append(sendAttempts, that.sendAttempts)
                .append(sendAttemptsMax, that.sendAttemptsMax)
                .append(conversationId, that.conversationId)
                .append(fromPartyId, that.fromPartyId)
                .append(toPartyId, that.toPartyId)
                .append(originalSender, that.originalSender)
                .append(finalRecipient, that.finalRecipient)
                .append(refToMessageId, that.refToMessageId)
                .append(messageId, that.messageId)
                .append(messageStatus, that.messageStatus)
                .append(notificationStatus, that.notificationStatus)
                .append(mshRole, that.mshRole)
                .append(messageType, that.messageType)
                .append(deleted, that.deleted)
                .append(received, that.received)
                .append(nextAttempt, that.nextAttempt)
                .append(failed, that.failed)
                .append(restored, that.restored)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(conversationId)
                .append(fromPartyId)
                .append(toPartyId)
                .append(originalSender)
                .append(finalRecipient)
                .append(refToMessageId)
                .append(messageId)
                .append(messageStatus)
                .append(notificationStatus)
                .append(mshRole)
                .append(messageType)
                .append(deleted)
                .append(received)
                .append(sendAttempts)
                .append(sendAttemptsMax)
                .append(nextAttempt)
                .append(failed)
                .append(restored)
                .toHashCode();
    }

    public static String csvTitle() {
        return new StringBuilder()
                .append("Conversation Id").append(",")
                .append("From Party Id").append(",")
                .append("To Party Id").append(",")
                .append("Original Sender").append(",")
                .append("Final Recipient").append(",")
                .append("Ref To Message Id").append(",")
                .append("Message Id").append(",")
                .append("Message Status").append(",")
                .append("Notification Status").append(",")
                .append("MSH Role").append(",")
                .append("Message Type").append(",")
                .append("Deleted").append(",")
                .append("Received").append(",")
                .append("Send Attempts").append(",")
                .append("Max Send Attempts").append(",")
                .append("Next Attempt").append(",")
                .append("Failed").append(",")
                .append("Restored")
                .append(System.lineSeparator())
                .toString();
    }

    public String toCsvString() {
        return new StringBuilder()
                .append(Objects.toString(conversationId,"")).append(",")
                .append(Objects.toString(fromPartyId,"")).append(",")
                .append(Objects.toString(toPartyId,"")).append(",")
                .append(Objects.toString(originalSender,"")).append(",")
                .append(Objects.toString(finalRecipient,"")).append(",")
                .append(Objects.toString(refToMessageId,"")).append(",")
                .append(Objects.toString(messageId, "")).append(",")
                .append(Objects.toString(messageStatus, "")).append(",")
                .append(Objects.toString(notificationStatus, "")).append(",")
                .append(Objects.toString(mshRole, "")).append(",")
                .append(Objects.toString(messageType,"")).append(",")
                .append(Objects.toString(deleted,"")).append(",")
                .append(Objects.toString(received, "")).append(",")
                .append(Objects.toString(sendAttempts, "")).append(",")
                .append(Objects.toString(sendAttemptsMax,"")).append(",")
                .append(Objects.toString(nextAttempt,"")).append(",")
                .append(Objects.toString(failed,"")).append(",")
                .append(Objects.toString(restored,""))
                .append(System.lineSeparator())
                .toString();
    }
}
