package eu.domibus.web.rest;

import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.services.AuditService;
import eu.domibus.core.message.MessageConverterService;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.PartInfo;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by musatmi on 10/05/2017.
 */
@RestController
@RequestMapping(value = "/rest/message")
public class MessageResource {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(MessageResource.class);

    @Autowired
    UserMessageService userMessageService;

    @Autowired
    MessageConverterService messageConverterService;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private AuditService auditService;


    @RequestMapping(path = "/restore", method = RequestMethod.PUT)
    public void resend(@RequestParam(value = "messageId", required = true) String messageId) {
        userMessageService.restoreFailedMessage(messageId);
        auditService.addMessageResentAudit(messageId);
    }

    @RequestMapping(path = "/{messageId:.+}/downloadOld", method = RequestMethod.GET)
    public ResponseEntity<ByteArrayResource> download(@PathVariable(value = "messageId") String messageId) {

        UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        final InputStream content = getMessage(userMessage);

        ByteArrayResource resource = new ByteArrayResource(new byte[0]);
        if (content != null) {
            try {
                resource = new ByteArrayResource(IOUtils.toByteArray(content));
            } catch (IOException e) {
                LOGGER.error("Error getting input stream for message [{}]", messageId);
            }
        }

        auditService.addMessageDownloadedAudit(messageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header("content-disposition", "attachment; filename=" + messageId + ".xml")
                .body(resource);
    }

    @RequestMapping(value = "/download")
    public ResponseEntity<ByteArrayResource> zipFiles(@RequestParam(value = "messageId", required = true) String messageId) throws IOException {

        UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        if (userMessage == null)
            return ResponseEntity.notFound().build();
        final Map<String, InputStream> message = getMessageWithAttachments(userMessage);
        byte[] zip = zip(message);
        auditService.addMessageDownloadedAudit(messageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header("content-disposition", "attachment; filename=" + messageId + ".zip")
                .body(new ByteArrayResource(zip));
    }

    public InputStream getMessage(UserMessage userMessage) {

        Messaging message = new Messaging();
        message.setUserMessage(userMessage);

        return new ByteArrayInputStream(messageConverterService.getAsByteArray(message));
    }

    private Map<String, InputStream> getMessageWithAttachments(UserMessage userMessage) {

        Map<String, InputStream> ret = new HashMap<>();

        final List<PartInfo> partInfo = userMessage.getPayloadInfo().getPartInfo();
        for (PartInfo info : partInfo) {
            try {
                ret.put(info.getHref().replace("cid:", ""), info.getPayloadDatahandler().getInputStream());
            } catch (IOException e) {
                LOGGER.error("Error getting input stream for attachment [{}]", info.getHref());
            }
        }

        ret.put("message.xml", getMessage(userMessage));


        return ret;
    }

    private byte[] zip(Map<String, InputStream> message) throws IOException {
        //creating byteArray stream, make it bufferable and passing this buffer to ZipOutputStream
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
             ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {

            for (Map.Entry<String, InputStream> entry : message.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

                IOUtils.copy(entry.getValue(), zipOutputStream);

                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();
            zipOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

}
