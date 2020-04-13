package eu.domibus.web.rest;

import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.core.message.MessagesLogService;
import eu.domibus.core.message.MessagingDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.message.converter.MessageConverterService;
import eu.domibus.web.rest.error.ErrorHandlerService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@RunWith(JMockit.class)
public class MessageResourceTest {

    @Tested
    MessageResource messageResource;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    MessageConverterService messageConverterService;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private AuditService auditService;

    @Injectable
    MessagesLogService messagesLogService;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Test
    public void testDownload() {
        // Given
        new Expectations() {{
            userMessageService.getMessageAsBytes(anyString);
            result = new byte[]{0, 1, 2};
        }};

        // When
        ResponseEntity<ByteArrayResource> responseEntity = messageResource.download("messageId");

        // Then
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assert.assertEquals(2, responseEntity.getHeaders().size());
        Assert.assertEquals("application/octet-stream", responseEntity.getHeaders().get("Content-Type").get(0));
        Assert.assertEquals("attachment; filename=messageId.xml", responseEntity.getHeaders().get("content-disposition").get(0));
    }

    @Test
    public void testDonloadZipped() throws IOException {
        // Given
        new Expectations() {{
            userMessageService.getMessageWithAttachmentsAsZip(anyString);
            result = new byte[]{0, 1, 2};
        }};

        ResponseEntity<ByteArrayResource> responseEntity = null;
        try {
            // When
            responseEntity = messageResource.donloadUserMessage("messageId");
        } catch (IOException | MessageNotFoundException e) {
            // NOT Then :)
            Assert.fail("Exception in zipFiles method");
        }

        // Then
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assert.assertEquals("application/zip", responseEntity.getHeaders().get("Content-Type").get(0));
        Assert.assertEquals("attachment; filename=messageId.zip", responseEntity.getHeaders().get("content-disposition").get(0));
    }

    @Test
    public void testReSend() {
        String messageId = UUID.randomUUID().toString();
        messageResource.resend(messageId);
        new Verifications() {{
            final String messageIdActual;
            final String messageIdActual1;
            userMessageService.resendFailedOrSendEnqueuedMessage(messageIdActual = withCapture());
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);

//            auditService.addMessageResentAudit(messageIdActual1 = withCapture());
//            Assert.assertEquals(messageId, messageIdActual1);
//            times = 1;
        }};
    }

}
