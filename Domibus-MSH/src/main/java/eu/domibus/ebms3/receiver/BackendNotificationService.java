package eu.domibus.ebms3.receiver;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.routing.BackendFilter;
import eu.domibus.api.routing.RoutingCriteria;
import eu.domibus.common.*;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.model.logging.MessageLog;
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
    UserMessageServiceHelper userMessageServiceHelper;

    @Autowired
    private UIReplicationSignalService uiReplicationSignalService;

    @Autowired
    protected List<BackendConnector> backendConnectors;


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
        final HashMap<String, Object> properties = new HashMap<>();
        if (errorResult.getErrorCode() != null) {
            properties.put(MessageConstants.ERROR_CODE, errorResult.getErrorCode().getErrorCodeName());
        }
        properties.put(MessageConstants.ERROR_DETAIL, errorResult.getErrorDetail());
        NotificationType notificationType = NotificationType.MESSAGE_RECEIVED_FAILURE;
        if(userMessage.isUserMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_RECEIVED_FAILURE;
        }

        notifyOfIncoming(userMessage, notificationType, properties);
    }

    public void notifyMessageReceived(final BackendFilter matchingBackendFilter, final UserMessage userMessage) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        NotificationType notificationType = NotificationType.MESSAGE_RECEIVED;
        if(userMessage.isUserMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_RECEIVED;
        }

        notifyOfIncoming(matchingBackendFilter, userMessage, notificationType, new HashMap<String, Object>());
    }

    public void notifyPayloadSubmitted(final UserMessage userMessage, String originalFilename, PartInfo partInfo, String backendName) {
        final BackendConnector backendConnector = getBackendConnector(backendName);
        PayloadSubmittedEvent payloadSubmittedEvent = new PayloadSubmittedEvent();
        payloadSubmittedEvent.setCid(partInfo.getHref());
        payloadSubmittedEvent.setFileName(originalFilename);
        payloadSubmittedEvent.setMessageId(userMessage.getMessageInfo().getMessageId());
        payloadSubmittedEvent.setMime(partInfo.getMime());
        backendConnector.payloadSubmitted(payloadSubmittedEvent);

    }

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
        LOG.info("Notifying backend [{}] of message [{}] and notification type [{}]", backendName, userMessage.getMessageInfo().getMessageId(), notificationType);

        validateSubmission(userMessage, backendName, notificationType);
        String finalRecipient = userMessageServiceHelper.getFinalRecipient(userMessage);
        if (properties != null) {
            properties.put(MessageConstants.FINAL_RECIPIENT, finalRecipient);
        }
        notify(userMessage.getMessageInfo().getMessageId(), backendName, notificationType, properties);
    }

    protected void notify(String messageId, String backendName, NotificationType notificationType) {
        notify(messageId, backendName, notificationType, null);
    }

    protected void notify(String messageId, String backendName, NotificationType notificationType, Map<String, Object> properties) {
        NotificationListener notificationListener = getNotificationListener(backendName);
        if (notificationListener == null) {
            LOG.warn("No notification listeners found for backend [" + backendName + "]");
            return;
        }

        LOG.debug("Required notifications [{}]", notificationListener.getRequiredNotificationTypeList());
        if (!notificationListener.getRequiredNotificationTypeList().contains(notificationType)) {
            LOG.debug("No plugin notification sent for message [{}]. Notification type [{}], mode [{}]", messageId, notificationType, notificationListener.getMode());
            return;
        }

        if (properties != null) {
            String finalRecipient = (String) properties.get(MessageConstants.FINAL_RECIPIENT);
            LOG.info("Notifying plugin [{}] for message [{}] with notificationType [{}] and finalRecipient [{}]", backendName, messageId, notificationType, finalRecipient);
        } else {
            LOG.info("Notifying plugin [{}] for message [{}] with notificationType [{}]", backendName, messageId, notificationType);
        }

        jmsManager.sendMessageToQueue(new NotifyMessageCreator(messageId, notificationType, properties).createMessage(), notificationListener.getBackendNotificationQueue());
    }

    public void notifyOfSendFailure(UserMessage userMessage) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        final String messageId = userMessage.getMessageInfo().getMessageId();
        final String backendName = userMessageLogDao.findBackendForMessageId(messageId);
        NotificationType notificationType = NotificationType.MESSAGE_SEND_FAILURE;
        if(userMessage.isUserMessageFragment()) {
            notificationType = NotificationType.MESSAGE_FRAGMENT_SEND_FAILURE;
        }

        notify(messageId, backendName, notificationType);
        userMessageLogDao.setAsNotified(messageId);

        uiReplicationSignalService.messageNotificationStatusChange(messageId, NotificationStatus.NOTIFIED);
    }

    public void notifyOfSendSuccess(final String messageId) {
        if (isPluginNotificationDisabled()) {
            return;
        }
        final String backendName = userMessageLogDao.findBackendForMessageId(messageId);
        notify(messageId, backendName, NotificationType.MESSAGE_SEND_SUCCESS);
        userMessageLogDao.setAsNotified(messageId);

        uiReplicationSignalService.messageNotificationStatusChange(messageId, NotificationStatus.NOTIFIED);
    }

    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public void notifyOfMessageStatusChange(MessageLog messageLog, MessageStatus newStatus, Timestamp changeTimestamp) {

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
        final Map<String, Object> messageProperties = getMessageProperties(messageLog, newStatus, changeTimestamp);
        notify(messageLog.getMessageId(), messageLog.getBackend(), NotificationType.MESSAGE_STATUS_CHANGE, messageProperties);
    }

    protected Map<String, Object> getMessageProperties(MessageLog messageLog, MessageStatus newStatus, Timestamp changeTimestamp) {
        Map<String, Object> properties = new HashMap<>();
        if (messageLog.getMessageStatus() != null) {
            properties.put("fromStatus", messageLog.getMessageStatus().toString());
        }
        properties.put("toStatus", newStatus.toString());
        properties.put("changeTimestamp", changeTimestamp.getTime());

        final UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageLog.getMessageId());
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
        return !domibusPropertyProvider.getBooleanProperty("domibus.plugin.notification.active");
    }
}
