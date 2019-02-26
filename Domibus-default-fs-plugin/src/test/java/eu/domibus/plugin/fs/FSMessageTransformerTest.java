package eu.domibus.plugin.fs;

import eu.domibus.plugin.Submission;
import eu.domibus.plugin.fs.ebms3.*;
import eu.domibus.plugin.fs.exception.FSPluginException;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public class FSMessageTransformerTest {

    private static final String INITIATOR_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator";
    private static final String RESPONDER_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";

    private static final String UNREGISTERED_PARTY_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    private static final String ORIGINAL_SENDER = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1";
    private static final String PROPERTY_ORIGINAL_SENDER = "originalSender";
    private static final String FINAL_RECIPIENT = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4";
    private static final String PROPERTY_FINAL_RECIPIENT = "finalRecipient";

    private static final String DOMIBUS_BLUE = "domibus-blue";
    private static final String DOMIBUS_RED = "domibus-red";

    private static final String SERVICE_NOPROCESS = "bdx:noprocess";
    private static final String CONTENT_ID = "cid:message";
    private static final String CONTENT_ID_MYPAYLOAD = "cid:mypayload";
    private static final String ACTION_TC1LEG1 = "TC1Leg1";
    private static final String SERVICE_TYPE_TC1 = "tc1";
    private static final String CONVERSATIONID_CONV1 = "conv1";
    private static final String MIME_TYPE = "MimeType";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_XML = "text/xml";
    private static final String AGREEMENT_REF_A1 = "A1";
    private static final String EMPTY_STR = "";
    private static final String MYPROP = "MyProp";
    private static final String MYPROP_TYPE = "propType";
    private static final String MYPROP_VALUE = "SomeValue";
    private static final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTransformFromSubmission_NormalFlow() throws Exception {
        String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        String conversationId = "ae413adb-920c-4d9c-a5a7-b5b2596eaf1c@domibus.eu";
        String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";

        // Submission
        Submission submission = new Submission();
        submission.setMessageId(messageId);
        submission.addFromParty(DOMIBUS_BLUE, UNREGISTERED_PARTY_TYPE);
        submission.setFromRole(INITIATOR_ROLE);
        submission.addToParty(DOMIBUS_RED, UNREGISTERED_PARTY_TYPE);
        submission.setToRole(RESPONDER_ROLE);

        submission.setServiceType(SERVICE_TYPE_TC1);
        submission.setService(SERVICE_NOPROCESS);
        submission.setAction(ACTION_TC1LEG1);
        submission.setAgreementRefType(EMPTY_STR);
        submission.setAgreementRef(AGREEMENT_REF_A1);
        submission.setConversationId(conversationId);

        submission.addMessageProperty(PROPERTY_ORIGINAL_SENDER, ORIGINAL_SENDER);
        submission.addMessageProperty(PROPERTY_FINAL_RECIPIENT, FINAL_RECIPIENT);

        ByteArrayDataSource dataSource = new ByteArrayDataSource(payloadContent.getBytes(), APPLICATION_XML);
        dataSource.setName("content.xml");
        DataHandler payLoadDataHandler = new DataHandler(dataSource);
        Submission.TypedProperty submissionTypedProperty = new Submission.TypedProperty(MIME_TYPE, APPLICATION_XML);
        Collection<Submission.TypedProperty> listTypedProperty = new ArrayList<>();
        listTypedProperty.add(submissionTypedProperty);
        Submission.Payload submissionPayload = new Submission.Payload(CONTENT_ID, payLoadDataHandler, listTypedProperty, false, null, null);
        submission.addPayload(submissionPayload);

        // Transform FSMessage from Submission
        FSMessageTransformer transformer = new FSMessageTransformer();
        FSMessage fsMessage = transformer.transformFromSubmission(submission, null);

        // Expected results for FSMessage
        UserMessage userMessage = fsMessage.getMetadata();
        From from = userMessage.getPartyInfo().getFrom();
        Assert.assertEquals(UNREGISTERED_PARTY_TYPE, from.getPartyId().getType());
        Assert.assertEquals(DOMIBUS_BLUE, from.getPartyId().getValue());
        Assert.assertEquals(INITIATOR_ROLE, from.getRole());

        To to = userMessage.getPartyInfo().getTo();
        Assert.assertEquals(UNREGISTERED_PARTY_TYPE, to.getPartyId().getType());
        Assert.assertEquals(DOMIBUS_RED, to.getPartyId().getValue());
        Assert.assertEquals(RESPONDER_ROLE, to.getRole());

        CollaborationInfo collaborationInfo = userMessage.getCollaborationInfo();
        Assert.assertEquals(SERVICE_TYPE_TC1, collaborationInfo.getService().getType());
        Assert.assertEquals(SERVICE_NOPROCESS, collaborationInfo.getService().getValue());
        Assert.assertEquals(ACTION_TC1LEG1, collaborationInfo.getAction());
        Assert.assertEquals(EMPTY_STR, collaborationInfo.getAgreementRef().getType());
        Assert.assertEquals(AGREEMENT_REF_A1, collaborationInfo.getAgreementRef().getValue());

        List<Property> propertyList = userMessage.getMessageProperties().getProperty();
        Assert.assertEquals(2, propertyList.size());
        Property property0 = propertyList.get(0);
        Assert.assertEquals(PROPERTY_ORIGINAL_SENDER, property0.getName());
        Assert.assertEquals(ORIGINAL_SENDER, property0.getValue());
        Property property1 = propertyList.get(1);
        Assert.assertEquals(PROPERTY_FINAL_RECIPIENT, property1.getName());
        Assert.assertEquals(FINAL_RECIPIENT, property1.getValue());

        FSPayload fSPayload = fsMessage.getPayloads().get(CONTENT_ID);
        Assert.assertEquals(APPLICATION_XML, fSPayload.getMimeType());
        Assert.assertEquals(payloadContent, IOUtils.toString(fSPayload.getDataHandler().getInputStream()));
    }

    @Test
    public void testTransformToSubmission_NormalFlow() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_metadata.xml");
        // Transform FSMessage to Submission
        FSMessageTransformer transformer = new FSMessageTransformer();
        Submission submission = transformer.transformToSubmission(fsMessage);

        assertTransformValues(submission);
        assertDefaultValuesForPayloadInfo(submission);
    }

    @Test
    public void testTransformToSubmission_NormalFlow_WithPayloadInfo() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_WithPayloadInfo_metadata.xml");
        // Transform FSMessage to Submission
        FSMessageTransformer transformer = new FSMessageTransformer();
        Submission submission = transformer.transformToSubmission(fsMessage);

        assertTransformPayloadInfo(submission);

    }

    @Test
    public void testTransformToFromToSubmission_NormalFlow() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_WithPayloadInfo_metadata.xml");
        // Transform FSMessage to Submission
        FSMessageTransformer transformer = new FSMessageTransformer();
        // perform the transformation to - from and back to subbmission
        Submission submission0 = transformer.transformToSubmission(fsMessage);
        FSMessage fsMessage1 = transformer.transformFromSubmission(submission0, null);
        Submission submission = transformer.transformToSubmission(fsMessage1);

        assertTransformValues(submission);
        assertTransformPayloadInfo(submission);
    }

    @Test(expected = FSPluginException.class)
    public void testTransformToSubmission_MultiplePartInfo() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_MultiplePartInfo_metadata.xml");
        FSMessageTransformer transformer = new FSMessageTransformer();
        // expect exception on multiple PartInfo in PayloadInfo
        transformer.transformToSubmission(fsMessage);
    }

    protected void assertDefaultValuesForPayloadInfo(Submission submission) throws IOException {
        Assert.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();
        Submission.TypedProperty payloadProperty = submissionPayload.getPayloadProperties().iterator().next();
        Assert.assertEquals(MIME_TYPE, payloadProperty.getKey());
        Assert.assertEquals(APPLICATION_XML, payloadProperty.getValue());

        DataHandler payloadDatahandler = submissionPayload.getPayloadDatahandler();
        Assert.assertEquals(APPLICATION_XML, payloadDatahandler.getContentType());
        Assert.assertEquals(payloadContent, IOUtils.toString(payloadDatahandler.getInputStream()));
    }

    protected void assertTransformPayloadInfo(Submission submission) {
        Assert.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();
        Assert.assertEquals(CONTENT_ID_MYPAYLOAD, submissionPayload.getContentId());

        Assert.assertEquals(3, submissionPayload.getPayloadProperties().size());
        for (Submission.TypedProperty payloadProperty : submissionPayload.getPayloadProperties()) {
            if (MIME_TYPE.equals(payloadProperty.getKey())) {
                Assert.assertEquals(TEXT_XML, payloadProperty.getValue());
            }
            if (MYPROP.equals(payloadProperty.getKey())) {
                Assert.assertEquals(MYPROP_TYPE, payloadProperty.getType());
                Assert.assertEquals(MYPROP_VALUE, payloadProperty.getValue());
            }
        }
    }

    protected void assertTransformValues(Submission submission) throws IOException {

        Assert.assertNotNull(submission);
        Assert.assertEquals(1, submission.getFromParties().size());
        Submission.Party fromParty = submission.getFromParties().iterator().next();
        Assert.assertEquals(DOMIBUS_BLUE, fromParty.getPartyId());
        Assert.assertEquals(UNREGISTERED_PARTY_TYPE, fromParty.getPartyIdType());
        Assert.assertEquals(INITIATOR_ROLE, submission.getFromRole());

        Assert.assertEquals(1, submission.getToParties().size());
        Submission.Party toParty = submission.getToParties().iterator().next();
        Assert.assertEquals(DOMIBUS_RED, toParty.getPartyId());
        Assert.assertEquals(UNREGISTERED_PARTY_TYPE, toParty.getPartyIdType());
        Assert.assertEquals(RESPONDER_ROLE, submission.getToRole());

        Assert.assertNull(submission.getAgreementRefType());
        Assert.assertNull(submission.getAgreementRef());
        Assert.assertEquals(SERVICE_NOPROCESS, submission.getService());
        Assert.assertEquals(SERVICE_TYPE_TC1, submission.getServiceType());
        Assert.assertEquals(ACTION_TC1LEG1, submission.getAction());
        Assert.assertEquals(CONVERSATIONID_CONV1, submission.getConversationId());

        Assert.assertEquals(2, submission.getMessageProperties().size());
        for (Submission.TypedProperty typedProperty : submission.getMessageProperties()) {
            if (PROPERTY_ORIGINAL_SENDER.equalsIgnoreCase(typedProperty.getKey())) {
                Assert.assertEquals(ORIGINAL_SENDER, typedProperty.getValue());
            }
            if (PROPERTY_FINAL_RECIPIENT.equalsIgnoreCase(typedProperty.getKey())) {
                Assert.assertEquals(FINAL_RECIPIENT, typedProperty.getValue());
            }
        }

        Assert.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();

        DataHandler payloadDatahandler = submissionPayload.getPayloadDatahandler();
        Assert.assertEquals(APPLICATION_XML, payloadDatahandler.getContentType());
        Assert.assertEquals(payloadContent, IOUtils.toString(payloadDatahandler.getInputStream()));

    }

    private FSMessage buildMessage(String filename) throws JAXBException {
        UserMessage metadata = FSTestHelper.getUserMessage(this.getClass(), filename);

        ByteArrayDataSource dataSource = new ByteArrayDataSource(payloadContent.getBytes(), APPLICATION_XML);
        dataSource.setName("content.xml");
        DataHandler dataHandler = new DataHandler(dataSource);
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(null, dataSource.getName(), dataHandler));
        FSMessage fsMessage = new FSMessage(fsPayloads, metadata);

        return fsMessage;
    }
}