package eu.domibus.plugin.webService.impl;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.*;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import eu.domibus.plugin.webService.generated.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.apache.commons.lang.StringUtils.trim;

/**
 * Converter class for Submission <-> UserMessage objects.
 *
 * @author Federico Martini
 */
@Component
public class StubDtoTransformer implements MessageSubmissionTransformer<Messaging>, MessageRetrievalTransformer<UserMessage> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(StubDtoTransformer.class);

    @Override
    public UserMessage transformFromSubmission(final Submission submission, final UserMessage target) {
        return transformFromSubmission(submission);
    }

    /**
     * Used to convert from the Domibus DTO object to the UserMessage stub object.
     *
     * @param submission
     * @return
     */
    public UserMessage transformFromSubmission(final Submission submission) {
        final UserMessage result = new UserMessage();
        this.generateCollaborationInfo(submission, result);
        this.generateMessageInfo(submission, result);
        this.generatePartyInfo(submission, result);
        this.generatePayload(submission, result);
        this.generateMessageProperties(submission, result);
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
        collaborationInfo.setConversationId(submission.getConversationId());
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
        messageInfo.setMessageId(submission.getMessageId());
        LOG.debug("MESSAGE ID " + messageInfo.getMessageId());
        GregorianCalendar gc = new GregorianCalendar();
        //messageInfo.setTimestamp(new XMLGregorianCalendarImpl(gc));
        LOG.debug("TIMESTAMP " + messageInfo.getTimestamp());
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
            from.setPartyId(partyId);
        }
        partyInfo.setFrom(from);

        final To to = new To();
        to.setRole(submission.getToRole());
        for (final Submission.Party party : submission.getToParties()) {
            final PartyId partyId = new PartyId();
            partyId.setValue(party.getPartyId());
            partyId.setType(party.getPartyIdType());
            to.setPartyId(partyId);
        }
        partyInfo.setTo(to);

        result.setPartyInfo(partyInfo);
    }


    private void generatePayload(final Submission submission, final UserMessage result) {

        final PayloadInfo payloadInfo = new PayloadInfo();

        for (final Submission.Payload payload : submission.getPayloads()) {
            final ExtendedPartInfo partInfo = new ExtendedPartInfo();
            partInfo.setInBody(payload.isInBody());
            partInfo.setPayloadDatahandler(payload.getPayloadDatahandler());
            partInfo.setHref(payload.getContentId());
            final PartProperties partProperties = new PartProperties();
            for (final Submission.TypedProperty entry : payload.getPayloadProperties()) {
                final Property property = new Property();
                property.setName(entry.getKey());
                property.setValue(entry.getValue());
                property.setType(entry.getType());
                partProperties.getProperty().add(property);
            }
            partInfo.setPartProperties(partProperties);
            payloadInfo.getPartInfo().add(partInfo);
            result.setPayloadInfo(payloadInfo);
        }


    }



    @Override
    @Transactional(propagation = Propagation.SUPPORTS,noRollbackFor = {IllegalArgumentException.class,IllegalStateException.class})
    public Submission transformToSubmission(final Messaging messageData) {
        return transformFromMessaging(messageData.getUserMessage());
    }


    /**
     * Used to convert from the UserMessage stub object to the Domibus DTO object.
     *
     * @param messaging
     * @return
     */
    public Submission transformFromMessaging(final UserMessage messaging) {
        LOG.debug("Entered method: transformFromMessaging(final UserMessage messaging)");

        final Submission result = new Submission();

        final CollaborationInfo collaborationInfo = messaging.getCollaborationInfo();
        result.setAction(trim(collaborationInfo.getAction()));
        result.setService(trim(messaging.getCollaborationInfo().getService().getValue()));
        result.setServiceType(trim(messaging.getCollaborationInfo().getService().getType()));
        if (collaborationInfo.getAgreementRef() != null) {
            result.setAgreementRef(trim(collaborationInfo.getAgreementRef().getValue()));
            result.setAgreementRefType(trim(collaborationInfo.getAgreementRef().getType()));
        }
        result.setConversationId(trim(collaborationInfo.getConversationId()));

        result.setMessageId(messaging.getMessageInfo().getMessageId());  //not trimming message id as non printable special characters needs to be checked.
        result.setRefToMessageId(trim(messaging.getMessageInfo().getRefToMessageId()));

        if (messaging.getPayloadInfo() != null) {
            for (final PartInfo partInfo : messaging.getPayloadInfo().getPartInfo()) {
                ExtendedPartInfo extPartInfo = (ExtendedPartInfo) partInfo;
                final Collection<Submission.TypedProperty> properties = new ArrayList<>();
                if (extPartInfo.getPartProperties() != null) {
                    for (final Property property : extPartInfo.getPartProperties().getProperty()) {
                        properties.add(new Submission.TypedProperty(trim(property.getName()), trim(property.getValue()), trim(property.getType())));
                    }
                }
                Submission.Description description = null;
                result.addPayload(extPartInfo.getHref(), extPartInfo.getPayloadDatahandler(), properties, extPartInfo.isInBody(), null, /*(partInfo.getSchema() != null) ? partInfo.getSchema().getLocation() :*/ null);
            }
        }

        if(messaging.getPartyInfo() != null && messaging.getPartyInfo().getFrom() != null) {
            PartyId partyId = messaging.getPartyInfo().getFrom().getPartyId();
            if(partyId != null) {
                result.addFromParty(trim(partyId.getValue()), trim(partyId.getType()));
            }
            result.setFromRole(trim(messaging.getPartyInfo().getFrom().getRole()));
        }
        if(messaging.getPartyInfo() != null && messaging.getPartyInfo().getTo() != null) {
            PartyId partyId = messaging.getPartyInfo().getTo().getPartyId();
            if(partyId != null) {
                result.addToParty(trim(partyId.getValue()), trim(partyId.getType()));
            }
            result.setToRole(trim(messaging.getPartyInfo().getTo().getRole()));
        }

        if (messaging.getMessageProperties() != null) {
            for (final Property property : messaging.getMessageProperties().getProperty()) {
                result.addMessageProperty(trim(property.getName()), trim(property.getValue()), trim(property.getType()));
            }
        }

        return result;
    }

    public MessageStatus transformFromMessageStatus(eu.domibus.common.MessageStatus messageStatus) {
        return MessageStatus.fromValue(messageStatus.name());
    }

    public ErrorResultImplArray transformFromErrorResults(List<? extends ErrorResult> errors) {
        ErrorResultImplArray errorList = new ErrorResultImplArray();
        for (ErrorResult errorResult : errors) {
            ErrorResultImpl errorResultImpl = new ErrorResultImpl();
            errorResultImpl.setErrorCode(ErrorCode.fromValue(errorResult.getErrorCode().name()));
            errorResultImpl.setErrorDetail(errorResult.getErrorDetail());
            errorResultImpl.setMshRole(MshRole.fromValue(errorResult.getMshRole().name()));
            errorResultImpl.setMessageInErrorId(errorResult.getMessageInErrorId());
            GregorianCalendar gc = new GregorianCalendar();
            if (errorResult.getNotified() != null) {
                gc.setTime(errorResult.getNotified());
            }
            //errorResultImpl.setNotified(new XMLGregorianCalendarImpl(gc));
            if (errorResult.getTimestamp() != null) {
                gc.setTime(errorResult.getTimestamp());
            }
            //errorResultImpl.setTimestamp(new XMLGregorianCalendarImpl(gc));
            errorList.getItem().add(errorResultImpl);
        }
        return errorList;
    }

}
