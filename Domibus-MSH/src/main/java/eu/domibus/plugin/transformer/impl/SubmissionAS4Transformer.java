package eu.domibus.plugin.transformer.impl;

import eu.domibus.common.services.impl.MessageIdGenerator;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.plugin.Submission;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author Christian Koch, Stefan Mueller
 */
@org.springframework.stereotype.Service
public class SubmissionAS4Transformer {

    @Autowired
    private MessageIdGenerator messageIdGenerator;

    // TODO: at least the following 2 methods have duplicate code with the one in StubDtoTransformer class.
    // Also, the method names look similar but they are not under the same interface; Maybe we should refactor and eliminate the duplication
    public UserMessage transformFromSubmission(final Submission submission) {
        final UserMessage result = new UserMessage();
        this.generateCollaborationInfo(submission, result);
        this.generateMessageInfo(submission, result);
        this.generatePartyInfo(submission, result);
        this.generatePayload(submission, result);
        this.generateMessageProperties(submission, result);

        //TODO: set mpc from pmode

        return result;
    }

    private void generateMessageProperties(final Submission submission, final UserMessage result) {

        final MessageProperties messageProperties = new MessageProperties();

        for (Submission.TypedProperty propertyEntry : submission.getMessageProperties()) {
            final Property prop = new Property();
            prop.setName(propertyEntry.getKey());
            prop.setValue(propertyEntry.getValue());
            prop.setType(propertyEntry.getType());
            messageProperties.getProperty().add(prop);
        }

        result.setMessageProperties(messageProperties);
    }

    private void generateCollaborationInfo(final Submission submission, final UserMessage result) {
        final CollaborationInfo collaborationInfo = new CollaborationInfo();
        // if the conversation id is null, we generate one; otherwise we trim it and pass it forward
        String conversationId = submission.getConversationId();
        collaborationInfo.setConversationId(conversationId == null ? this.generateConversationId() : conversationId.trim());
        collaborationInfo.setAction(submission.getAction());
        final AgreementRef agreementRef = new AgreementRef();
        agreementRef.setValue(submission.getAgreementRef());
        agreementRef.setType(submission.getAgreementRefType());
        collaborationInfo.setAgreementRef(agreementRef);
        final Service service = new Service();
        service.setValue(submission.getService());
        service.setType(submission.getServiceType());
        collaborationInfo.setService(service);
        result.setCollaborationInfo(collaborationInfo);
    }

    private void generateMessageInfo(final Submission submission, final UserMessage result) {
        final MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessageId((submission.getMessageId() != null && submission.getMessageId().trim().length() > 0) ? submission.getMessageId() : this.messageIdGenerator.generateMessageId());
        messageInfo.setTimestamp(new Date());
        messageInfo.setRefToMessageId(submission.getRefToMessageId());
        result.setMessageInfo(messageInfo);
    }

    private void generatePartyInfo(final Submission submission, final UserMessage result) {
        final PartyInfo partyInfo = new PartyInfo();
        final From from = new From();
        from.setRole(submission.getFromRole());
        for (final Submission.Party party : submission.getFromParties()) {
            final PartyId partyId = new PartyId();
            partyId.setValue(party.getPartyId());
            partyId.setType(party.getPartyIdType());
            from.getPartyId().add(partyId);
        }
        partyInfo.setFrom(from);

        final To to = new To();
        to.setRole(submission.getToRole());
        for (final Submission.Party party : submission.getToParties()) {
            final PartyId partyId = new PartyId();
            partyId.setValue(party.getPartyId());
            partyId.setType(party.getPartyIdType());
            to.getPartyId().add(partyId);
        }
        partyInfo.setTo(to);

        result.setPartyInfo(partyInfo);
    }


    private void generatePayload(final Submission submission, final UserMessage result) {
        final PayloadInfo payloadInfo = new PayloadInfo();


        for (final Submission.Payload payload : submission.getPayloads()) {
            final PartInfo partInfo = new PartInfo();
            partInfo.setInBody(payload.isInBody());
            partInfo.setPayloadDatahandler(payload.getPayloadDatahandler());
            partInfo.setHref(payload.getContentId());
            final PartProperties partProperties = new PartProperties();
            for (final Submission.TypedProperty entry : payload.getPayloadProperties()) {
                final Property property = new Property();
                property.setName(entry.getKey());
                property.setValue(entry.getValue());
                property.setType(entry.getType());
                partProperties.getProperties().add(property);
            }

            partInfo.setPartProperties(partProperties);
            payloadInfo.getPartInfo().add(partInfo);

            result.setPayloadInfo(payloadInfo);
        }


    }

    public Submission transformFromMessaging(final UserMessage messaging) {
        final Submission result = new Submission();

        if (messaging == null) {
            return result;
        }

        final CollaborationInfo collaborationInfo = messaging.getCollaborationInfo();
        result.setAction(collaborationInfo.getAction());
        result.setService(messaging.getCollaborationInfo().getService().getValue());
        result.setServiceType(messaging.getCollaborationInfo().getService().getType());
        if (collaborationInfo.getAgreementRef() != null) {
            result.setAgreementRef(collaborationInfo.getAgreementRef().getValue());
            result.setAgreementRefType(collaborationInfo.getAgreementRef().getType());
        }
        result.setConversationId(collaborationInfo.getConversationId());

        result.setMessageId(messaging.getMessageInfo().getMessageId());
        result.setRefToMessageId(messaging.getMessageInfo().getRefToMessageId());

        if (messaging.getPayloadInfo() != null) {
            for (final PartInfo partInfo : messaging.getPayloadInfo().getPartInfo()) {
                addPayload(result, partInfo);
            }
        }
        result.setFromRole(messaging.getPartyInfo().getFrom().getRole());
        result.setToRole(messaging.getPartyInfo().getTo().getRole());

        for (final PartyId partyId : messaging.getPartyInfo().getFrom().getPartyId()) {
            result.addFromParty(partyId.getValue(), partyId.getType());
        }

        for (final PartyId partyId : messaging.getPartyInfo().getTo().getPartyId()) {
            result.addToParty(partyId.getValue(), partyId.getType());
        }

        if (messaging.getMessageProperties() != null) {
            for (final Property property : messaging.getMessageProperties().getProperty()) {
                result.addMessageProperty(property.getName(), property.getValue(), property.getType());
            }
        }
        return result;
    }

    private void addPayload(Submission result, PartInfo partInfo) {
        final Collection<Submission.TypedProperty> properties = new ArrayList<>();
        if (partInfo.getPartProperties() != null) {
            for (final Property property : partInfo.getPartProperties().getProperties()) {
                properties.add(new Submission.TypedProperty(property.getName(), property.getValue(), property.getType()));
            }
        }
        if (partInfo.getFileName() != null) {
            properties.add(new Submission.TypedProperty("FileName", partInfo.getFileName(), null));
        }
        result.addPayload(partInfo.getHref(), partInfo.getPayloadDatahandler(), properties, partInfo.isInBody(), null, null);
    }

    private String generateConversationId() {
        return this.messageIdGenerator.generateMessageId();
    }
}
