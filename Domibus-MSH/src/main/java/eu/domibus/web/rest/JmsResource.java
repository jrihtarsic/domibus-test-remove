package eu.domibus.web.rest;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.common.services.impl.CsvServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(value = "/rest/jms")
public class JmsResource {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(JmsResource.class);

    private static final String APPLICATION_JSON = "application/json";

    @Autowired
    JMSManager jmsManager;

    @Autowired
    CsvServiceImpl csvServiceImpl;

    @RequestMapping(value = {"/destinations"}, method = GET)
    public ResponseEntity<DestinationsResponseRO> destinations() {

        final DestinationsResponseRO destinationsResponseRO = new DestinationsResponseRO();
        try {
            destinationsResponseRO.setJmsDestinations(jmsManager.getDestinations());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(destinationsResponseRO);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error finding the JMS messages sources", runEx);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(destinationsResponseRO);
        }
    }

    @RequestMapping(value = {"/messages"}, method = POST)
    public ResponseEntity<MessagesResponseRO> messages(@RequestBody MessagesRequestRO request) {

        final MessagesResponseRO messagesResponseRO = new MessagesResponseRO();
        try {
            messagesResponseRO.setMessages(jmsManager.browseMessages(request.getSource(), request.getJmsType(), request.getFromDate(), request.getToDate(), request.getSelector()));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(messagesResponseRO);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error browsing messages for source [" + request.getSource() + "]", runEx);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(messagesResponseRO);
        }
    }

    @RequestMapping(value = {"/messages/action"}, method = POST)
    public ResponseEntity<MessagesActionResponseRO> action(@RequestBody MessagesActionRequestRO request) {

        final MessagesActionResponseRO response = new MessagesActionResponseRO();
        response.setOutcome("Success");

        try {
            List<String> messageIds = request.getSelectedMessages();
            String[] ids = messageIds.toArray(new String[0]);

            if (request.getAction() == MessagesActionRequestRO.Action.MOVE) {
                jmsManager.moveMessages(request.getSource(), request.getDestination(), ids);

            } else if (request.getAction() == MessagesActionRequestRO.Action.REMOVE) {
                jmsManager.deleteMessages(request.getSource(), ids);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(response);

        } catch (RuntimeException runEx) {
            LOGGER.error("Error performing action [" + request.getAction() + "]", runEx);
            response.setOutcome(runEx.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType(APPLICATION_JSON))
                    .body(response);
        }
    }

    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(
            @RequestParam(value = "source") String source,
            @RequestParam(value = "jmsType", required = false) String jmsType,
            @RequestParam(value = "fromDate", required = false) Long fromDate,
            @RequestParam(value = "toDate", required = false) Long toDate,
            @RequestParam(value = "selector", required = false) String selector) {
        String resultText;

        final List<JmsMessage> jmsMessageList = jmsManager.browseMessages(source, jmsType, fromDate == null?null:new Date(fromDate), toDate == null?null:new Date(toDate), selector);
        List<String> excludedItems = new ArrayList<>();
        excludedItems.add("PROPERTY_ORIGINAL_QUEUE");
        csvServiceImpl.setExcludedItems(excludedItems);

        try {
            resultText = csvServiceImpl.exportToCSV(jmsMessageList);
        } catch (CsvException e) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/ms-excel"))
                .header("Content-Disposition", "attachment; filename=jms_datatable.csv")
                .body(resultText);
    }


}
