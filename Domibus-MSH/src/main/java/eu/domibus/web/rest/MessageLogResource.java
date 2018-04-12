package eu.domibus.web.rest;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.PartyDao;
import eu.domibus.common.dao.SignalMessageLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.logging.MessageLogInfo;
import eu.domibus.common.services.CsvService;
import eu.domibus.common.services.impl.CsvServiceImpl;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.web.rest.ro.MessageLogRO;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import eu.domibus.web.rest.ro.TestServiceMessageInfoRO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest/messagelog")
public class MessageLogResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageLogResource.class);

    private static final String MAXIMUM_NUMBER_CSV_ROWS = "domibus.ui.maximumcsvrows";
    private static final String RECEIVED_FROM_STR = "receivedFrom";
    private static final String RECEIVED_TO_STR = "receivedTo";

    @Autowired
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private SignalMessageLogDao signalMessageLogDao;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private PartyDao partyDao;

    @Autowired
    DateUtil dateUtil;

    @Autowired
    CsvServiceImpl csvServiceImpl;

    //significant improvements to the query execution plan have been found by always passing the date.
    //so we provide a default from and to.
    Date defaultFrom;

    Date defaultTo;

    @PostConstruct
    protected void init() {
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        try {
            defaultFrom = ft.parse("1977-10-25");
            defaultTo = ft.parse("2977-10-25");
        } catch (ParseException e) {
            LOGGER.error("Impossible to initiate default dates");
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public MessageLogResultRO getMessageLog(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "orderBy", required = false) String column,
            @RequestParam(value = "asc", defaultValue = "true") boolean asc,
            @RequestParam(value = "messageId", required = false) String messageId,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "mshRole", required = false) MSHRole mshRole,
            @RequestParam(value = "messageType", defaultValue = "USER_MESSAGE") MessageType messageType,
            @RequestParam(value = "messageStatus", required = false) MessageStatus messageStatus,
            @RequestParam(value = "notificationStatus", required = false) NotificationStatus notificationStatus,
            @RequestParam(value = "fromPartyId", required = false) String fromPartyId,
            @RequestParam(value = "toPartyId", required = false) String toPartyId,
            @RequestParam(value = "refToMessageId", required = false) String refToMessageId,
            @RequestParam(value = "originalSender", required = false) String originalSender,
            @RequestParam(value = "finalRecipient", required = false) String finalRecipient,
            @RequestParam(value = RECEIVED_FROM_STR, required = false) String receivedFrom,
            @RequestParam(value = RECEIVED_TO_STR, required = false) String receivedTo,
            @RequestParam(value = "messageSubtype", required = false) MessageSubtype messageSubtype) {

        LOGGER.debug("Getting message log");

        MessageLogResultRO result = new MessageLogResultRO();

        //TODO why are those filters send back to the GUI??
        HashMap<String, Object> filters = createFilterMap(messageId, conversationId, mshRole, messageStatus, notificationStatus, fromPartyId, toPartyId, refToMessageId, originalSender, finalRecipient, messageSubtype);

        Date from = dateUtil.fromString(receivedFrom);
        if (from == null) {
            from = defaultFrom;
        }
        Date to = dateUtil.fromString(receivedTo);
        if (to == null) {
            to = defaultTo;
        }
        filters.put(RECEIVED_FROM_STR, from);
        filters.put(RECEIVED_TO_STR, to);

        result.setFilter(filters);
        LOGGER.debug("using filters [{}]", filters);

        List<MessageLogInfo> resultList = new ArrayList<>();
        if (messageType == MessageType.SIGNAL_MESSAGE) {
            int numberOfSignalMessageLogs = signalMessageLogDao.countAllInfo(asc, filters);
            LOGGER.debug("count Signal Messages Logs [{}]", numberOfSignalMessageLogs);
            result.setCount(numberOfSignalMessageLogs);
            resultList = signalMessageLogDao.findAllInfoPaged(pageSize * page, pageSize, column, asc, filters);

        } else if (messageType == MessageType.USER_MESSAGE) {
            int numberOfUserMessageLogs = userMessageLogDao.countAllInfo(asc, filters);
            LOGGER.debug("count User Messages Logs [{}]", numberOfUserMessageLogs);
            result.setCount(numberOfUserMessageLogs);
            resultList = userMessageLogDao.findAllInfoPaged(pageSize * page, pageSize, column, asc, filters);
        }
        //needed here because the info is not needed for the queries but is used by the gui as the filter is returned with
        //the result. Why??.
        filters.put("messageType", messageType);
        if (filters.get(RECEIVED_FROM_STR).equals(defaultFrom)) {
            filters.remove(RECEIVED_FROM_STR);
        }
        if (filters.get(RECEIVED_TO_STR).equals(defaultTo)) {
            filters.remove(RECEIVED_TO_STR);
        }
        result.setMessageLogEntries(convertMessageLogInfoList(resultList));
        result.setMshRoles(MSHRole.values());
        result.setMsgTypes(MessageType.values());
        result.setMsgStatus(MessageStatus.values());
        result.setNotifStatus(NotificationStatus.values());
        result.setPage(page);
        result.setPageSize(pageSize);

        return result;
    }

    /**
     * This method returns a CSV file with the contents of Messages table
     *
     * @return CSV file with the contents of Messages table
     */
    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(
            @RequestParam(value = "messageId", required = false) String messageId,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "mshRole", required = false) MSHRole mshRole,
            @RequestParam(value = "messageType", defaultValue = "USER_MESSAGE") MessageType messageType,
            @RequestParam(value = "messageStatus", required = false) MessageStatus messageStatus,
            @RequestParam(value = "notificationStatus", required = false) NotificationStatus notificationStatus,
            @RequestParam(value = "fromPartyId", required = false) String fromPartyId,
            @RequestParam(value = "toPartyId", required = false) String toPartyId,
            @RequestParam(value = "refToMessageId", required = false) String refToMessageId,
            @RequestParam(value = "originalSender", required = false) String originalSender,
            @RequestParam(value = "finalRecipient", required = false) String finalRecipient,
            @RequestParam(value = RECEIVED_FROM_STR, required = false) String receivedFrom,
            @RequestParam(value = RECEIVED_TO_STR, required = false) String receivedTo,
            @RequestParam(value = "messageSubtype", required = false) MessageSubtype messageSubtype) {
        HashMap<String, Object> filters = createFilterMap(messageId, conversationId, mshRole, messageStatus, notificationStatus, fromPartyId, toPartyId, refToMessageId, originalSender, finalRecipient, messageSubtype);
        Date from = dateUtil.fromString(receivedFrom);
        if (from == null) {
            from = defaultFrom;
        }
        Date to = dateUtil.fromString(receivedTo);
        if (to == null) {
            to = defaultTo;
        }
        filters.put(RECEIVED_FROM_STR, from);
        filters.put(RECEIVED_TO_STR, to);

        int maxCSVrows = Integer.parseInt(domibusProperties.getProperty(MAXIMUM_NUMBER_CSV_ROWS, String.valueOf(CsvService.MAX_NUMBER_OF_ENTRIES)));

        List<MessageLogInfo> resultList = new ArrayList<>();
        if (messageType == MessageType.SIGNAL_MESSAGE) {
            resultList = signalMessageLogDao.findAllInfoPaged(0, maxCSVrows, null, true, filters);
        } else if (messageType == MessageType.USER_MESSAGE) {
            resultList = userMessageLogDao.findAllInfoPaged(0, maxCSVrows, null, true, filters);
        }

        // needed for empty csv file purposes
        csvServiceImpl.setClass(MessageLogInfo.class);

        // column customization
        csvServiceImpl.customizeColumn(CsvCustomColumns.MESSAGE_RESOURCE.getCustomColumns());

        String resultText;
        try {
            resultText = csvServiceImpl.exportToCSV(resultList);
        } catch (CsvException e) {
            LOGGER.error("Exception caught during export to CSV", e);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CsvService.APPLICATION_EXCEL_STR))
                .header("Content-Disposition", "attachment; filename=" + csvServiceImpl.getCsvFilename("messages"))
                .body(resultText);
    }

    @RequestMapping(value = "lastTestSent", method = RequestMethod.GET)
    public TestServiceMessageInfoRO getLastTestSent(@RequestParam(value = "partyId") String partyId) {
        LOGGER.debug("Getting last sent test message for partyId='{}'", partyId);

        String userMessageId = userMessageLogDao.findLastUserTestMessageId(partyId);
        UserMessage userMessageByMessageId = messagingDao.findUserMessageByMessageId(userMessageId);

        TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
        testServiceMessageInfoRO.setMessageId(userMessageId);
        testServiceMessageInfoRO.setTimeReceived(userMessageByMessageId.getMessageInfo().getTimestamp());
        testServiceMessageInfoRO.setPartyId(partyId);
        Party party = partyDao.findById(partyId);
        testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());

        return testServiceMessageInfoRO;
    }

    @RequestMapping(value = "lastTestReceived", method = RequestMethod.GET)
    public TestServiceMessageInfoRO getLastTestReceived(@RequestParam(value = "partyId") String partyId, @RequestParam(value = "userMessageId") String userMessageId) {
        LOGGER.debug("Getting last received test message from partyId='{}'", partyId);
        Messaging messaging = messagingDao.findMessageByMessageId(userMessageId);
        String signalMessageId = messaging.getSignalMessage().getMessageInfo().getMessageId();
        SignalMessage signalMessage = messagingDao.findSignalMessageByMessageId(signalMessageId);

        TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
        testServiceMessageInfoRO.setMessageId(signalMessageId);
        testServiceMessageInfoRO.setTimeReceived(signalMessage.getMessageInfo().getTimestamp());

        Party party = partyDao.findById(partyId);
        testServiceMessageInfoRO.setPartyId(partyId);
        testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());

        return testServiceMessageInfoRO;
    }

    protected List<MessageLogRO> convertMessageLogInfoList(List<MessageLogInfo> objects) {
        List<MessageLogRO> result = new ArrayList<>();
        for(MessageLogInfo object : objects) {
            final MessageLogRO messageLogRO = convertMessageLogInfo(object);
            if(messageLogRO != null) {
                result.add(messageLogRO);
            }
        }
        return result;
    }

    private HashMap<String, Object> createFilterMap(@RequestParam(value = "messageId", required = false) String messageId, @RequestParam(value = "conversationId", required = false) String conversationId, @RequestParam(value = "mshRole", required = false) MSHRole mshRole, @RequestParam(value = "messageStatus", required = false) MessageStatus messageStatus, @RequestParam(value = "notificationStatus", required = false) NotificationStatus notificationStatus, @RequestParam(value = "fromPartyId", required = false) String fromPartyId, @RequestParam(value = "toPartyId", required = false) String toPartyId, @RequestParam(value = "refToMessageId", required = false) String refToMessageId, @RequestParam(value = "originalSender", required = false) String originalSender, @RequestParam(value = "finalRecipient", required = false) String finalRecipient, @RequestParam(value = "messageSubtype")MessageSubtype messageSubtype) {
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageId", messageId);
        filters.put("conversationId", conversationId);
        filters.put("mshRole", mshRole);
        filters.put("messageStatus", messageStatus);
        filters.put("notificationStatus", notificationStatus);
        filters.put("fromPartyId", fromPartyId);
        filters.put("toPartyId", toPartyId);
        filters.put("refToMessageId", refToMessageId);
        filters.put("originalSender", originalSender);
        filters.put("finalRecipient", finalRecipient);
        filters.put("messageSubtype", messageSubtype);
        return filters;
    }

    private MessageLogRO convertMessageLogInfo(MessageLogInfo messageLogInfo) {
        if(messageLogInfo == null) {
            return null;
        }

        MessageLogRO result = new MessageLogRO();
        result.setConversationId(messageLogInfo.getConversationId());
        result.setFromPartyId(messageLogInfo.getFromPartyId());
        result.setToPartyId(messageLogInfo.getToPartyId());
        result.setOriginalSender(messageLogInfo.getOriginalSender());
        result.setFinalRecipient(messageLogInfo.getFinalRecipient());
        result.setRefToMessageId(messageLogInfo.getRefToMessageId());
        result.setMessageId(messageLogInfo.getMessageId());
        result.setMessageStatus(messageLogInfo.getMessageStatus());
        result.setNotificationStatus(messageLogInfo.getNotificationStatus());
        result.setMshRole(messageLogInfo.getMshRole());
        result.setMessageType(messageLogInfo.getMessageType());
        result.setDeleted(messageLogInfo.getDeleted());
        result.setReceived(messageLogInfo.getReceived());
        result.setSendAttempts(messageLogInfo.getSendAttempts());
        result.setSendAttemptsMax(messageLogInfo.getSendAttemptsMax());
        result.setNextAttempt(messageLogInfo.getNextAttempt());
        result.setFailed(messageLogInfo.getFailed());
        result.setRestored(messageLogInfo.getRestored());
        result.setMessageSubtype(messageLogInfo.getMessageSubtype());
        return result;
    }
}
