package eu.domibus.ebms3.receiver;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.metrics.MetricsHelper;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.routing.BackendFilter;
import eu.domibus.api.routing.RoutingCriteria;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.*;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.services.impl.UserMessageHandlerService;
import eu.domibus.common.services.impl.UserMessageHandlerServiceImpl;
import eu.domibus.core.alerts.model.service.MessagingModuleConfiguration;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.core.alerts.service.MultiDomainAlertConfigurationService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.ebms3.common.UserMessageServiceHelper;
import eu.domibus.ebms3.common.model.PartInfo;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.NotifyMessageCreator;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.plugin.NotificationListener;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.routing.BackendFilterEntity;
import eu.domibus.plugin.routing.CriteriaFactory;
import eu.domibus.plugin.routing.IRoutingCriteria;
import eu.domibus.plugin.routing.RoutingService;
import eu.domibus.plugin.routing.dao.BackendFilterDao;
import eu.domibus.plugin.transformer.impl.SubmissionAS4Transformer;
import eu.domibus.plugin.validation.SubmissionValidator;
import eu.domibus.plugin.validation.SubmissionValidatorList;
import eu.domibus.submission.SubmissionValidatorListProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jms.Queue;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_PLUGIN_NOTIFICATION_ACTIVE;

/**
 * @author Christian Koch, Stefan Mueller
 * @author Cosmin Baciu
 */
@Service("backendNotificationService")
public class BackendNotificationService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(BackendNotificationService.class);

    @Autowired
    JMSManager jmsManager;

    @Autowired
    private BackendFilterDao backendFilterDao;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    protected SubmissionAS4Transformer submissionAS4Transformer;

    @Autowired
    protected SubmissionValidatorListProvider submissionValidatorListProvider;

    protected List<NotificationListener> notificationListenerServices;

    @Resource(name = "routingCriteriaFactories")
    protected List<CriteriaFactory> routingCriteriaFactories;

    @Autowired
    @Qualifier("unknownReceiverQueue")
    private Queue unknownReceiverQueue;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private DomainCoreConverter coreConverter;

    @Autowired
    private EventService eventService;

    @Autowired
    private MultiDomainAlertConfigurationService multiDomainAlertConfigurationService;

    @Autowired
    private UserMessageServiceHelper userMessageServiceHelper;

    @Autowired
    private UserMessageHandlerService userMessageHandlerService;

    @Autowired
    private UIReplicationSignalService uiReplicationSignalService;

    @Autowired
    protected List<BackendConnector> backendConnectors;

    @Autowired
    protected UserMessageService userMessageService;

    @Autowired
    private MetricRegistry metricRegistry;


    //TODO move this into a dedicate provider(a different spring bean class)
    private Map<String, IRoutingCriteria> criteriaMap;


    @PostConstruct
    public void init() {
        Map notificationListenerBeanMap = applicationContext.getBeansOfType(NotificationListener.class);
        if (notificationListenerBeanMap.isEmpty()) {
            throw new ConfigurationException("No Plugin available! Please configure at least one backend plugin in order to run domibus");
        } else {
            notificationListenerServices = new ArrayList<NotificationListener>(notificationListenerBeanMap.values());
        }

        criteriaMap = new HashMap<>();
        for (final CriteriaFactory routingCriteriaFactory : routingCriteriaFactories) {
            criteriaMap.put(routingCriteriaFactory.getName(), routingCriteriaFactory.getInstance());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMessageReceivedFailure(final UserMessage userMessage, ErrorResult errorResult) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        final Map<String, Object> properties = new HashMap<>();
        if (errorResult.getErrorCode() != null) {
            properties.put(MessageConstants.ERROR_CODE, errorResult.getErrorCode().getErrorCodeName());
        }
        properties.put(MessageConstants.ERROR_DETAIL, errorResult.getErrorDetail());
        NotificationType notificationType = NotificationType.MESSAGE_RECEIVED_FAILURE;
        if (userMessage.isUserMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_RECEIVED_FAILURE;
        }

        notifyOfIncoming(userMessage, notificationType, properties);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void notifyMessageReceived(final BackendFilter matchingBackendFilter, final UserMessage userMessage) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        NotificationType notificationType = NotificationType.MESSAGE_RECEIVED;
        if (userMessage.isUserMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_RECEIVED;
        }

        notifyOfIncoming(matchingBackendFilter, userMessage, notificationType, new HashMap<String, Object>());
    }

    public void notifyPayloadSubmitted(final UserMessage userMessage, String originalFilename, PartInfo partInfo, String backendName) {
        if (userMessageHandlerService.checkTestMessage(userMessage)) {
            LOG.debug("Payload submitted notifications are not enabled for test messages [{}]", userMessage);
            return;
        }

        final BackendConnector backendConnector = getBackendConnector(backendName);
        PayloadSubmittedEvent payloadSubmittedEvent = new PayloadSubmittedEvent();
        payloadSubmittedEvent.setCid(partInfo.getHref());
        payloadSubmittedEvent.setFileName(originalFilename);
        payloadSubmittedEvent.setMessageId(userMessage.getMessageInfo().getMessageId());
        payloadSubmittedEvent.setMime(partInfo.getMime());
        backendConnector.payloadSubmittedEvent(payloadSubmittedEvent);
    }

    public void notifyPayloadProcessed(final UserMessage userMessage, String originalFilename, PartInfo partInfo, String backendName) {
        if (userMessageHandlerService.checkTestMessage(userMessage)) {
            LOG.debug("Payload processed notifications are not enabled for test messages [{}]", userMessage);
            return;
        }

        final BackendConnector backendConnector = getBackendConnector(backendName);
        PayloadProcessedEvent payloadProcessedEvent = new PayloadProcessedEvent();
        payloadProcessedEvent.setCid(partInfo.getHref());
        payloadProcessedEvent.setFileName(originalFilename);
        payloadProcessedEvent.setMessageId(userMessage.getMessageInfo().getMessageId());
        payloadProcessedEvent.setMime(partInfo.getMime());
        backendConnector.payloadProcessedEvent(payloadProcessedEvent);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public BackendFilter getMatchingBackendFilter(final UserMessage userMessage) {
        List<BackendFilter> backendFilters = getBackendFilters();
        return getMatchingBackendFilter(backendFilters, criteriaMap, userMessage);
    }

    protected void notifyOfIncoming(final BackendFilter matchingBackendFilter, final UserMessage userMessage, final NotificationType notificationType, Map<String, Object> properties) {
        if (matchingBackendFilter == null) {
            LOG.error("No backend responsible for message [" + userMessage.getMessageInfo().getMessageId() + "] found. Sending notification to [" + unknownReceiverQueue + "]");
            String finalRecipient = userMessageServiceHelper.getFinalRecipient(userMessage);
            properties.put(MessageConstants.FINAL_RECIPIENT, finalRecipient);
            jmsManager.sendMessageToQueue(new NotifyMessageCreator(userMessage.getMessageInfo().getMessageId(), notificationType, properties).createMessage(), unknownReceiverQueue);
            return;
        }

        validateAndNotify(userMessage, matchingBackendFilter.getBackendName(), notificationType, properties);
    }

    protected void notifyOfIncoming(final UserMessage userMessage, final NotificationType notificationType, Map<String, Object> properties) {
        final BackendFilter matchingBackendFilter = getMatchingBackendFilter(userMessage);
        notifyOfIncoming(matchingBackendFilter, userMessage, notificationType, properties);
    }

    protected BackendFilter getMatchingBackendFilter(final List<BackendFilter> backendFilters, final Map<String, IRoutingCriteria> criteriaMap, final UserMessage userMessage) {
        LOG.debug("Getting the backend filter for message [" + userMessage.getMessageInfo().getMessageId() + "]");
        for (final BackendFilter filter : backendFilters) {
            final boolean backendFilterMatching = isBackendFilterMatching(filter, criteriaMap, userMessage);
            if (backendFilterMatching) {
                LOG.debug("Filter [" + filter + "] matched for message [" + userMessage.getMessageInfo().getMessageId() + "]");
                return filter;
            }
        }
        return null;
    }

    protected boolean isBackendFilterMatching(BackendFilter filter, Map<String, IRoutingCriteria> criteriaMap, final UserMessage userMessage) {
        if (filter.getRoutingCriterias() != null) {
            for (final RoutingCriteria routingCriteriaEntity : filter.getRoutingCriterias()) {
                final IRoutingCriteria criteria = criteriaMap.get(StringUtils.upperCase(routingCriteriaEntity.getName()));
                boolean matches = criteria.matches(userMessage, routingCriteriaEntity.getExpression());
                //if at least one criteria does not match it means the filter is not matching
                if (!matches) {
                    return false;
                }
            }
        }
        return true;
    }

    protected List<BackendFilter> getBackendFilters() {
        List<BackendFilterEntity> backendFilterEntities = backendFilterDao.findAll();

        if (!backendFilterEntities.isEmpty()) {
            return coreConverter.convert(backendFilterEntities, BackendFilter.class);
        }

        List<BackendFilter> backendFilters = routingService.getBackendFilters();
        if (backendFilters.isEmpty()) {
            LOG.error("There are no backend plugins deployed on this server");
        }
        if (backendFilters.size() > 1) { //There is more than one unconfigured backend available. For security reasons we cannot send the message just to the first one
            LOG.error("There are multiple unconfigured backend plugins available. Please set up the configuration using the \"Message filter\" pannel of the administrative GUI.");
            backendFilters.clear(); // empty the list so its handled in the desired way.
        }
        //If there is only one backend deployed we send it to that as this is most likely the intent
        return backendFilters;
    }

    protected void validateSubmission(UserMessage userMessage, String backendName, NotificationType notificationType) {
        if (NotificationType.MESSAGE_RECEIVED != notificationType) {
            LOG.debug("Validation is not configured to be done for notification of type [" + notificationType + "]");
            return;
        }

        SubmissionValidatorList submissionValidatorList = submissionValidatorListProvider.getSubmissionValidatorList(backendName);
        if (submissionValidatorList == null) {
            LOG.debug("No submission validators found for backend [" + backendName + "]");
            return;
        }
        LOG.info("Performing submission validation for backend [" + backendName + "]");
        Submission submission = submissionAS4Transformer.transformFromMessaging(userMessage);
        List<SubmissionValidator> submissionValidators = submissionValidatorList.getSubmissionValidators();
        for (SubmissionValidator submissionValidator : submissionValidators) {
            submissionValidator.validate(submission);
        }
    }

    protected NotificationListener getNotificationListener(String backendName) {
        for (final NotificationListener notificationListenerService : notificationListenerServices) {
            if (notificationListenerService.getBackendName().equalsIgnoreCase(backendName)) {
                return notificationListenerService;
            }
        }
        return null;
    }

    protected BackendConnector getBackendConnector(String backendName) {
        for (final BackendConnector backendConnector : backendConnectors) {
            if (backendConnector.getName().equalsIgnoreCase(backendName)) {
                return backendConnector;
            }
        }
        return null;
    }

    protected void validateAndNotify(UserMessage userMessage, String backendName, NotificationType notificationType, Map<String, Object> properties) {
        com.codahale.metrics.Timer.Context validateAndNotifyTimer = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(BackendNotificationService.class, "validateAndNotify.timer")).time();
        com.codahale.metrics.Counter validateAndNotifyCounter = MetricsHelper.getMetricRegistry().counter(MetricRegistry.name(BackendNotificationService.class, "validateAndNotify.counter"));
        validateAndNotifyCounter.inc();
        LOG.info("Notifying backend [{}] of message [{}] and notification type [{}]", backendName, userMessage.getMessageInfo().getMessageId(), notificationType);

        com.codahale.metrics.Timer.Context validateSubmissionTimer = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(BackendNotificationService.class, "validateSubmission.timer")).time();
        validateSubmission(userMessage, backendName, notificationType);
        validateSubmissionTimer.stop();
        com.codahale.metrics.Timer.Context finalRecipientTimer = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(BackendNotificationService.class, "finalRecipient.timer")).time();
        String finalRecipient = userMessageServiceHelper.getFinalRecipient(userMessage);
        if (properties != null) {
            properties.put(MessageConstants.FINAL_RECIPIENT, finalRecipient);
        }
        finalRecipientTimer.stop();
        notify(userMessage.getMessageInfo().getMessageId(), backendName, notificationType, properties);
        validateAndNotifyTimer.stop();
        validateAndNotifyCounter.dec();
    }

    protected void notify(String messageId, String backendName, NotificationType notificationType) {
        notify(messageId, backendName, notificationType, null);
    }

    protected void notify(String messageId, String backendName, NotificationType notificationType, Map<String, Object> properties) {
        com.codahale.metrics.Timer.Context notificationListenerTimer = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(BackendNotificationService.class, "notificationListener.timer")).time();
        NotificationListener notificationListener = getNotificationListener(backendName);
        if (notificationListener == null) {
            LOG.warn("No notification listeners found for backend [" + backendName + "]");
            return;
        }
        notificationListenerTimer.stop();

        com.codahale.metrics.Timer.Context requireNotificationTimer = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(BackendNotificationService.class, "requireNotification.timer")).time();
        LOG.debug("Required notifications [{}]", notificationListener.getRequiredNotificationTypeList());
        if (!notificationListener.getRequiredNotificationTypeList().contains(notificationType)) {
            LOG.debug("No plugin notification sent for message [{}]. Notification type [{}], mode [{}]", messageId, notificationType, notificationListener.getMode());
            return;
        }
        requireNotificationTimer.stop();

        if (properties != null) {
            String finalRecipient = (String) properties.get(MessageConstants.FINAL_RECIPIENT);
            LOG.info("Notifying plugin [{}] for message [{}] with notificationType [{}] and finalRecipient [{}]", backendName, messageId, notificationType, finalRecipient);
        } else {
            LOG.info("Notifying plugin [{}] for message [{}] with notificationType [{}]", backendName, messageId, notificationType);
        }

        com.codahale.metrics.Timer.Context prepareMessageTimer = metricRegistry.timer(MetricRegistry.name(BackendNotificationService.class, "prepareJms.timer")).time();
        JmsMessage message = new NotifyMessageCreator(messageId, notificationType, properties).createMessage();
        prepareMessageTimer.stop();

        com.codahale.metrics.Timer.Context getQueueTimer = metricRegistry.timer(MetricRegistry.name(BackendNotificationService.class, "getQueue.timer")).time();
        Queue backendNotificationQueue = notificationListener.getBackendNotificationQueue();
        getQueueTimer.stop();


        com.codahale.metrics.Timer.Context sendMessageToQueue = metricRegistry.timer(MetricRegistry.name(BackendNotificationService.class, "sendMessageToQueue")).time();
        jmsManager.sendMessageToQueue(message, backendNotificationQueue);
        sendMessageToQueue.stop();
    }

    public void notifyOfSendFailure(UserMessageLog userMessageLog) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        final String messageId = userMessageLog.getMessageId();
        final String backendName = userMessageLog.getBackend();
        NotificationType notificationType = NotificationType.MESSAGE_SEND_FAILURE;
        if (userMessageLog.getMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_SEND_FAILURE;
        }

        notify(messageId, backendName, notificationType);
        userMessageLogDao.setAsNotified(userMessageLog);

        uiReplicationSignalService.messageNotificationStatusChange(messageId, NotificationStatus.NOTIFIED);
    }

    public void notifyOfSendSuccess(final UserMessageLog userMessageLog) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        String messageId = userMessageLog.getMessageId();
        NotificationType notificationType = NotificationType.MESSAGE_SEND_SUCCESS;
        if (userMessageLog.getMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_SEND_SUCCESS;
        }

        notify(messageId, userMessageLog.getBackend(), notificationType);
        userMessageLogDao.setAsNotified(userMessageLog);

        uiReplicationSignalService.messageNotificationStatusChange(messageId, NotificationStatus.NOTIFIED);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public void notifyOfMessageStatusChange(UserMessageLog messageLog, MessageStatus newStatus, Timestamp changeTimestamp) {
        notifyOfMessageStatusChange(null, messageLog, newStatus, changeTimestamp);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public void notifyOfMessageStatusChange(UserMessage userMessage, UserMessageLog messageLog, MessageStatus newStatus, Timestamp changeTimestamp) {
        final MessagingModuleConfiguration messagingConfiguration = multiDomainAlertConfigurationService.getMessageCommunicationConfiguration();

        if (messagingConfiguration.shouldMonitorMessageStatus(newStatus)) {
            eventService.enqueueMessageEvent(messageLog.getMessageId(), messageLog.getMessageStatus(), newStatus, messageLog.getMshRole());
        }

        if (isPluginNotificationDisabled()) {
            return;
        }
        final String messageId = messageLog.getMessageId();
        if (StringUtils.isNotBlank(messageId)) {
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);
        }
        if (messageLog.getMessageStatus() == newStatus) {
            LOG.debug("Notification not sent: message status has not changed [{}]", newStatus);
            return;
        }
        LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_STATUS_CHANGED, messageLog.getMessageStatus(), newStatus);

        //TODO check if it is needed
        if (userMessage == null) {
            LOG.debug("Getting UserMessage with id [{}]", messageId);
            userMessage = messagingDao.findUserMessageByMessageId(messageId);
        }

        final Map<String, Object> messageProperties = getMessageProperties(messageLog, userMessage, newStatus, changeTimestamp);
        NotificationType notificationType = NotificationType.MESSAGE_STATUS_CHANGE;
        if (messageLog.getMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_STATUS_CHANGE;
        }

        notify(messageLog.getMessageId(), messageLog.getBackend(), notificationType, messageProperties);
    }

    protected Map<String, Object> getMessageProperties(MessageLog messageLog, UserMessage userMessage, MessageStatus newStatus, Timestamp changeTimestamp) {
        Map<String, Object> properties = new HashMap<>();
        if (messageLog.getMessageStatus() != null) {
            properties.put("fromStatus", messageLog.getMessageStatus().toString());
        }
        properties.put("toStatus", newStatus.toString());
        properties.put("changeTimestamp", changeTimestamp.getTime());


        if (userMessage != null) {
            LOG.debug("Adding the service and action properties for message [{}]", messageLog.getMessageId());

            properties.put("service", userMessage.getCollaborationInfo().getService().getValue());
            properties.put("serviceType", userMessage.getCollaborationInfo().getService().getType());
            properties.put("action", userMessage.getCollaborationInfo().getAction());
        }
        return properties;
    }

    public List<NotificationListener> getNotificationListenerServices() {
        return notificationListenerServices;
    }

    protected boolean isPluginNotificationDisabled() {
        return !domibusPropertyProvider.getBooleanProperty(DOMIBUS_PLUGIN_NOTIFICATION_ACTIVE);
    }
}
