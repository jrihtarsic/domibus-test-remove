package eu.domibus.plugin.webService.logging;

import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ext.logging.AbstractLoggingInterceptor;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.helpers.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JMockit.class)
public class WSPluginLoggingEventHelperImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(WSPluginLoggingEventHelperImplTest.class);


    private static void logInfo(String test, String methodName, long before) {
        LOG.info("Test {}, method {} has spent {} milliseconds",
                test, methodName, System.currentTimeMillis() - before);
    }

    @Tested
    WSPluginLoggingEventHelperImpl wsPluginLoggingEventHelper;

    @Test
    public void test_stripPayload_SubmitMessage(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_SubmitMessage.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.REQ_IN;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_SUBMIT_MESSAGE;

            logEvent.getPayload();
            result = payload;
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_SubmitMessage", "stripPayload", before);

        new Verifications() {{
            final String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertNotNull(actualPayload);
            Assert.assertTrue(actualPayload.contains(AbstractLoggingInterceptor.CONTENT_SUPPRESSED));
        }};
    }

    @Test
    public void test_stripPayload_SubmitMessage_MultipleValues(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_SubmitMessage_MultiplePayloads.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.REQ_IN;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_SUBMIT_MESSAGE;

            logEvent.isMultipartContent();
            result = false;

            logEvent.getPayload();
            result = payload;
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_SubmitMessage_MultipleValues", "stripPayload", before);


        new FullVerifications() {{
            final String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertNotNull(actualPayload);
            Assert.assertEquals(3, StringUtils.countMatches(actualPayload, AbstractLoggingInterceptor.CONTENT_SUPPRESSED));
        }};
    }

    @Test
    public void test_stripPayload_SubmitMessage_MTOM(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_SubmitMessage_MTOM_Attachments.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.REQ_IN;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_SUBMIT_MESSAGE;

            logEvent.isMultipartContent();
            result = true;

            logEvent.getPayload();
            result = payload;

            logEvent.getContentType();
            result = "multipart/related; type=\"application/xop+xml\"; start=\"<rootpart@soapui.org>\"; start-info=\"application/soap+xml\"; action=\"\"; boundary=\"----=_Part_6_567004613.1582023394958\"";
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_SubmitMessage_MTOM", "stripPayload", before);


        new FullVerifications() {{
            final String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertNotNull(actualPayload);
            Assert.assertEquals(3, StringUtils.countMatches(actualPayload, AbstractLoggingInterceptor.CONTENT_SUPPRESSED));
        }};
    }

    @Test
    public void test_stripPayload_RetrieveMessage(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_RetrieveMessage.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.RESP_OUT;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_RETRIEVE_MESSAGE;

            logEvent.getPayload();
            result = payload;
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_RetrieveMessage", "stripPayload", before);


        new Verifications() {{
            final String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertNotNull(actualPayload);
            Assert.assertTrue(actualPayload.contains(AbstractLoggingInterceptor.CONTENT_SUPPRESSED));
        }};
    }

    @Test
    public void test_stripPayload_RetrieveMessage_2Attachments(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_RetrieveMessage_2Attachments.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.RESP_OUT;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_RETRIEVE_MESSAGE;

            logEvent.isMultipartContent();
            result = false;

            logEvent.getPayload();
            result = payload;
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_RetrieveMessage_2Attachments", "stripPayload", before);


        new Verifications() {{
            final String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertNotNull(actualPayload);
            Assert.assertTrue(actualPayload.contains(AbstractLoggingInterceptor.CONTENT_SUPPRESSED));
        }};
    }

    @Test
    public void test_stripPayload_SubmitMessage_NoContent(final @Mocked LogEvent logEvent) throws Exception {

        final String payload = readPayload("payload_SubmitMessage_no_content.xml");

        new Expectations() {{
            logEvent.getType();
            result = EventType.REQ_IN;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_SUBMIT_MESSAGE;

            logEvent.isMultipartContent();
            result = false;

            logEvent.getPayload();
            result = payload;
        }};

        //tested method
        long before = System.currentTimeMillis();
        wsPluginLoggingEventHelper.stripPayload(logEvent);
        logInfo("test_stripPayload_SubmitMessage_NoContent",
                "stripPayload", before);


        new Verifications() {{
            String actualPayload;
            logEvent.setPayload(actualPayload = withCapture());
            Assert.assertEquals(payload, actualPayload);
            times = 1;
        }};
    }

    @Test
    public void test_checkIfOperationIsAllowed(final @Mocked LogEvent logEvent) {
        new Expectations() {{
            logEvent.getType();
            result = EventType.REQ_IN;
            result = EventType.RESP_OUT;

            logEvent.getOperationName();
            result = WSPluginLoggingEventHelperImpl.OPERATION_SUBMIT_MESSAGE;
            result = WSPluginLoggingEventHelperImpl.OPERATION_RETRIEVE_MESSAGE;
        }};


        Assert.assertEquals(WSPluginLoggingEventHelperImpl.SUBMIT_REQUEST,
                wsPluginLoggingEventHelper.checkIfOperationIsAllowed(logEvent));
        Assert.assertEquals(WSPluginLoggingEventHelperImpl.RETRIEVE_MESSAGE_RESPONSE,
                wsPluginLoggingEventHelper.checkIfOperationIsAllowed(logEvent));
    }

    private String readPayload(final String payloadName) throws Exception {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(payloadName), "UTF-8");
    }

}