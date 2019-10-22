package eu.domibus.web.rest;

import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.logging.MessageLogInfo;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.services.MessagesLogService;
import eu.domibus.core.csv.CsvCustomColumns;
import eu.domibus.core.csv.CsvExcludedItems;
import eu.domibus.core.csv.CsvService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.core.replication.UIMessageService;
import eu.domibus.core.replication.UIReplicationSignalServiceImpl;
import eu.domibus.ebms3.common.model.MessageType;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Tiago Miguel, Catalin Enache
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest/messagelog")
@Validated
public class MessageLogResource extends BaseResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageLogResource.class);

    private static final String RECEIVED_FROM_STR = "receivedFrom";
    private static final String RECEIVED_TO_STR = "receivedTo";

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private DateUtil dateUtil;

    @Autowired
    private CsvServiceImpl csvServiceImpl;

    @Autowired
    private UIMessageService uiMessageService;

    @Autowired
    private MessagesLogService messagesLogService;

    @Autowired
    private UIReplicationSignalServiceImpl uiReplicationSignalService;

    Date defaultFrom;
    Date defaultTo;

    @PostConstruct
    public void init() {
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        try {
            defaultFrom = ft.parse("1970-01-01 23:59:00");
            defaultTo = ft.parse("2977-10-25 23:59:00");
        } catch (ParseException e) {
            LOG.error("Impossible to initiate default dates");
        }
    }

    @GetMapping
    public MessageLogResultRO getMessageLog(@Valid MessageLogFilterRequestRO request) {
        LOG.debug("Getting message log");

        //creating the filters
        HashMap<String, Object> filters = createFilterMap(request);

        //we just set default values for received column
        // in order to improve pagination on large amount of data
        Date from = dateUtil.fromString(request.getReceivedFrom());
        if (from == null) {
            from = defaultFrom;
        }
        Date to = dateUtil.fromString(request.getReceivedTo());
        if (to == null) {
            to = defaultTo;
        }
        filters.put(RECEIVED_FROM_STR, from);
        filters.put(RECEIVED_TO_STR, to);
        filters.put("messageType", request.getMessageType());

        LOG.debug("using filters [{}]", filters);

        MessageLogResultRO result;
        if (uiReplicationSignalService.isReplicationEnabled()) {
            /** use TB_MESSAGE_UI table instead */
            result = uiMessageService.countAndFindPaged(request.getPageSize() * request.getPage(), request.getPageSize(),
                    request.getOrderBy(), request.getAsc(), filters);
        } else {
            //old, fashioned way
            result = messagesLogService.countAndFindPaged(request.getMessageType(), request.getPageSize() * request.getPage(),
                    request.getPageSize(), request.getOrderBy(), request.getAsc(), filters);
        }

        if (defaultFrom.equals(from)) {
            filters.remove(RECEIVED_FROM_STR);
        }
        if (defaultTo.equals(to)) {
            filters.remove(RECEIVED_TO_STR);
        }
        result.setFilter(filters);
        result.setMshRoles(MSHRole.values());
        result.setMsgTypes(MessageType.values());
        result.setMsgStatus(MessageStatus.values());
        result.setNotifStatus(NotificationStatus.values());
        result.setPage(request.getPage());
        result.setPageSize(request.getPageSize());

        return result;
    }

    /**
     * This method returns a CSV file with the contents of Messages table
     *
     * @return CSV file with the contents of Messages table
     */
    @GetMapping(path = "/csv")
    public ResponseEntity<String> getCsv(@Valid MessageLogFilterRequestRO request) {
        HashMap<String, Object> filters = createFilterMap(request);

        filters.put(RECEIVED_FROM_STR, dateUtil.fromString(request.getReceivedFrom()));
        filters.put(RECEIVED_TO_STR, dateUtil.fromString(request.getReceivedTo()));
        filters.put("messageType", request.getMessageType());

        int maxNumberRowsToExport = csvServiceImpl.getMaxNumberRowsToExport();

        List<MessageLogInfo> resultList;
        if (uiReplicationSignalService.isReplicationEnabled()) {
            /** use TB_MESSAGE_UI table instead */
            resultList = uiMessageService.findPaged(0, maxNumberRowsToExport, request.getOrderBy(), request.getAsc(), filters);
        } else {
            resultList = messagesLogService.findAllInfoCSV(request.getMessageType(), maxNumberRowsToExport, request.getOrderBy(), request.getAsc(), filters);
        }

        return exportToCSV(resultList,
                MessageLogInfo.class,
                CsvCustomColumns.MESSAGE_RESOURCE.getCustomColumns(),
                CsvExcludedItems.MESSAGE_LOG_RESOURCE.getExcludedItems(),
                "messages");
    }

    @GetMapping(value = "test/outgoing/latest")
    public ResponseEntity<TestServiceMessageInfoRO> getLastTestSent(@Valid LatestOutgoingMessageRequestRO request) {
        String partyId = request.getPartyId();
        LOG.debug("Getting last sent test message for partyId='{}'", partyId);

        String userMessageId = userMessageLogDao.findLastUserTestMessageId(partyId);
        if (StringUtils.isBlank(userMessageId)) {
            LOG.debug("Could not find last user message id for party [{}]", partyId);
            return ResponseEntity.noContent().build();
        }

        UserMessageLog userMessageLog = null;
        //TODO create a UserMessageLog object independent of Hibernate annotations in the domibus-api and use the UserMessageLogService instead
        try {
            userMessageLog = userMessageLogDao.findByMessageId(userMessageId);
        } catch (NoResultException ex) {
            LOG.trace("No UserMessageLog found for message with id [{}]", userMessageId);
        }

        if (userMessageLog != null) {
            TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
            testServiceMessageInfoRO.setMessageId(userMessageId);
            testServiceMessageInfoRO.setTimeReceived(userMessageLog.getReceived());
            testServiceMessageInfoRO.setPartyId(partyId);
            Party party = pModeProvider.getPartyByIdentifier(partyId);
            testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());

            return ResponseEntity.ok().body(testServiceMessageInfoRO);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "test/incoming/latest")
    public ResponseEntity<TestServiceMessageInfoRO> getLastTestReceived(@Valid LatestIncomingMessageRequestRO request) {
        String partyId = request.getPartyId();
        String userMessageId = request.getUserMessageId();
        LOG.debug("Getting last received test message from partyId='{}'", partyId);

        Messaging messaging = messagingDao.findMessageByMessageId(userMessageId);
        if (messaging == null) {
            LOG.debug("Could not find messaging for message ID[{}]", userMessageId);
            return ResponseEntity.noContent().build();
        }

        SignalMessage signalMessage = messaging.getSignalMessage();
        if (signalMessage != null) {
            TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
            testServiceMessageInfoRO.setMessageId(signalMessage.getMessageInfo().getMessageId());
            testServiceMessageInfoRO.setTimeReceived(signalMessage.getMessageInfo().getTimestamp());
            Party party = pModeProvider.getPartyByIdentifier(partyId);
            testServiceMessageInfoRO.setPartyId(partyId);
            testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());

            return ResponseEntity.ok().body(testServiceMessageInfoRO);
        }

        return ResponseEntity.noContent().build();
    }

    private HashMap<String, Object> createFilterMap(MessageLogFilterRequestRO request) {
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageId", request.getMessageId());
        filters.put("conversationId", request.getConversationId());
        filters.put("mshRole", request.getMshRole());
        filters.put("messageStatus", request.getMessageStatus());
        filters.put("notificationStatus", request.getNotificationStatus());
        filters.put("fromPartyId", request.getFromPartyId());
        filters.put("toPartyId", request.getToPartyId());
        filters.put("refToMessageId", request.getRefToMessageId());
        filters.put("originalSender", request.getOriginalSender());
        filters.put("finalRecipient", request.getFinalRecipient());
        filters.put("messageSubtype", request.getMessageSubtype());
        return filters;
    }

    @Override
    public CsvService getCsvService() {
        return csvServiceImpl;
    }
}
