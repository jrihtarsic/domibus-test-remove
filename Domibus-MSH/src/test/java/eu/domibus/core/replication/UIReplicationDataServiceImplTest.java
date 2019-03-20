package eu.domibus.core.replication;

import eu.domibus.api.message.MessageSubtype;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.SignalMessageLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.SignalMessageLog;
import eu.domibus.common.model.logging.UserMessageLogEntity;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.ebms3.common.UserMessageDefaultServiceHelper;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Date;
import java.util.Random;
import java.util.UUID;

/**
 * JUnit for {@link UIReplicationDataServiceImpl} class
 *
 * @author Catalin Enache
 * @since 4.0
 */
@RunWith(JMockit.class)
public class UIReplicationDataServiceImplTest {

    @Tested
    UIReplicationDataServiceImpl uiReplicationDataService;

    @Injectable
    private UIMessageDaoImpl uiMessageDao;

    @Injectable
    private UIMessageDiffDao uiMessageDiffDao;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private MessagingDao messagingDao;

    @Injectable
    private SignalMessageLogDao signalMessageLogDao;

    @Injectable
    private UserMessageDefaultServiceHelper userMessageDefaultServiceHelper;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainCoreConverter domainConverter;

    private ObjectFactory ebmsObjectFactory = new ObjectFactory();
    private final String messageId = UUID.randomUUID().toString();
    private final MessageStatus messageStatus = MessageStatus.SEND_ENQUEUED;
    private final NotificationStatus notificationStatus = NotificationStatus.REQUIRED;
    private final MSHRole mshRole = MSHRole.SENDING;
    private final MessageType messageType = MessageType.USER_MESSAGE;
    private final String conversationId = UUID.randomUUID().toString();
    private final String refToMessageId = UUID.randomUUID().toString();
    private Random rnd = new Random();
    private final Date deleted = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
    private final Date received = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
    private final Date nextAttempt = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
    private final Date failed = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
    private final Date restored = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
    private final Date jmsTime = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));

    private final MessageSubtype messageSubtype = MessageSubtype.TEST;

    private final int sendAttempts = 1;
    private final int sendAttemptsMax = 5;

    private final String toPartyId = "domibus-red";
    private final String toPartyIdType = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    private final String fromPartyId = "domibus-blue";
    private final String fromPartyIdType = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    private final String originalSender = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1";
    private final String finalRecipient = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4";


    @Test
    public void testMessageReceived(final @Mocked UIMessageEntity uiMessageEntity, final @Injectable Mapper domainCoreConverter) {

        new Expectations(uiReplicationDataService) {{
            uiReplicationDataService.saveUIMessageFromUserMessageLog(anyString, jmsTime.getTime());
        }};

        //tested
        uiReplicationDataService.messageReceived(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            uiReplicationDataService.saveUIMessageFromUserMessageLog(messageIdActual = withCapture(), anyLong);
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void testMessageSubmitted() {
        new Expectations(uiReplicationDataService) {{
            uiReplicationDataService.saveUIMessageFromUserMessageLog(anyString, jmsTime.getTime());
        }};

        //tested
        uiReplicationDataService.messageSubmitted(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            uiReplicationDataService.saveUIMessageFromUserMessageLog(messageIdActual = withCapture(), anyLong);
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void testMessageStatusChange_EntityFound_ResultOK(final @Mocked UIMessageEntity uiMessageEntity) {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = uiMessageEntity;

            uiMessageEntity.getLastModified().getTime();
            result = jmsTime.getTime() - 20;
        }};

        //tested method
        uiReplicationDataService.messageStatusChange(messageId, messageStatus, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            MessageStatus messageStatusActual;
            Date deletedActual, nextAttemptActual, failedActual, lastModifiedActual;

            uiMessageDao.updateMessageStatus(messageIdActual = withCapture(), messageStatusActual = withCapture(),
                    deletedActual = withCapture(), nextAttemptActual = withCapture(), failedActual = withCapture(),
            withAny(new java.util.Date()));

            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(messageStatus, messageStatusActual);
            Assert.assertEquals(deleted, deletedActual);
            Assert.assertEquals(nextAttempt, nextAttemptActual);
            Assert.assertEquals(failed, failedActual);
        }};
    }

    @Test
    public void testMessageStatusChange_EntityNotFound_Warning() {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations() {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = null;
        }};

        //tested method
        uiReplicationDataService.messageStatusChange(messageId, messageStatus, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{

        }};
    }

    @Test
    public void testMessageNotificationStatusChange_EntityFound_ResultOK(final @Mocked UIMessageEntity uiMessageEntity) {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = uiMessageEntity;

            uiMessageEntity.getLastModified2().getTime();
            result = jmsTime.getTime() - 20;
        }};

        //tested method
        uiReplicationDataService.messageNotificationStatusChange(messageId, notificationStatus, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            NotificationStatus notificationStatusActual;

            uiMessageDao.updateNotificationStatus(messageIdActual = withCapture(),
                    notificationStatusActual = withCapture(), withAny(new java.util.Date()));

            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(notificationStatus, notificationStatusActual);

        }};
    }

    @Test
    public void testMessageNotificationStatusChange_EntityNotFound_Warning() {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = null;
        }};

        //tested method
        uiReplicationDataService.messageNotificationStatusChange(messageId, notificationStatus, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{

        }};
    }

    @Test
    public void testMessageChange_EntityFound_ResultOK(final @Mocked UIMessageEntity uiMessageEntity) {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = uiMessageEntity;
        }};

        //tested method
        uiReplicationDataService.messageChange(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            uiMessageDao.updateMessage(messageId, messageStatus, deleted, failed, restored, nextAttempt, sendAttempts, sendAttemptsMax, jmsTime);
        }};
    }

    @Test
    public void testMessageChange_EntityNotFound_Warn() {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(anyString);
            result = userMessageLog;

            uiMessageDao.findUIMessageByMessageId(anyString);
            result = null;
        }};

        //tested method
        uiReplicationDataService.messageChange(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
        }};
    }

    @Test
    public void testSignalMessageSubmitted() {

        new Expectations(uiReplicationDataService) {{
            uiReplicationDataService.saveUIMessageFromSignalMessageLog(anyString, jmsTime.getTime());
        }};

        //tested
        uiReplicationDataService.signalMessageSubmitted(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            uiReplicationDataService.saveUIMessageFromSignalMessageLog(messageIdActual = withCapture(), anyLong);
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void testSignalMessageReceived() {
        new Expectations(uiReplicationDataService) {{
            uiReplicationDataService.saveUIMessageFromSignalMessageLog(anyString, jmsTime.getTime());
        }};

        //tested
        uiReplicationDataService.signalMessageReceived(messageId, jmsTime.getTime());

        new FullVerifications(uiReplicationDataService) {{
            String messageIdActual;
            uiReplicationDataService.saveUIMessageFromSignalMessageLog(messageIdActual = withCapture(), anyLong);
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void testSaveUIMessageFromSignalMessageLog(final @Mocked UIMessageEntity uiMessageEntity) {
        final UserMessageLogEntity userMessageLog = createUserMessageLog();
        final UserMessage userMessage = createUserMessage();

        new Expectations(uiReplicationDataService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

            domainConverter.convert(userMessageLog, UIMessageEntity.class);
            result = uiMessageEntity;

            userMessageDefaultServiceHelper.getFinalRecipient(userMessage);
            result = finalRecipient;

            userMessageDefaultServiceHelper.getOriginalSender(userMessage);
            result = originalSender;

            uiMessageEntity.getLastModified();
            result = jmsTime;

        }};

        //tested method
        uiReplicationDataService.saveUIMessageFromUserMessageLog(messageId, jmsTime.getTime());

        new FullVerifications(uiMessageEntity) {{

            uiMessageEntity.setEntityId(anyInt);

            String actualValue;
            uiMessageEntity.setMessageId(actualValue = withCapture());
            Assert.assertEquals(messageId, actualValue);

            uiMessageEntity.setConversationId(actualValue = withCapture());
            Assert.assertEquals(conversationId, actualValue);

            uiMessageEntity.setFromId(actualValue = withCapture());
            Assert.assertEquals(fromPartyId, actualValue);

            uiMessageEntity.setToId(actualValue = withCapture());
            Assert.assertEquals(toPartyId, actualValue);

            uiMessageEntity.setFromScheme(actualValue = withCapture());
            Assert.assertEquals(originalSender, actualValue);

            uiMessageEntity.setToScheme(actualValue = withCapture());
            Assert.assertEquals(finalRecipient, actualValue);

            uiMessageEntity.setRefToMessageId(actualValue = withCapture());
            Assert.assertEquals(refToMessageId, actualValue);

            uiMessageEntity.setLastModified(withAny(new java.util.Date()));
            uiMessageEntity.setLastModified2(withAny(new java.util.Date()));


            final UIMessageEntity entityActual;
            uiMessageDao.create(entityActual = withCapture());
            Assert.assertNotNull(entityActual);

        }};

    }

    @Test
    public void testSaveUIMessageFromUserMessageLog(final @Mocked Messaging messaging, final @Mocked UIMessageEntity uiMessageEntity) {
        final SignalMessageLog signalMessageLog = createSignalMessageLog();
        final UserMessage userMessage = createUserMessage();
        final SignalMessage signalMessage = createSignalMessage();

        new Expectations(uiReplicationDataService) {{
            signalMessageLogDao.findByMessageId(messageId);
            result = signalMessageLog;

            messagingDao.findSignalMessageByMessageId(messageId);
            result = signalMessage;

            signalMessage.getMessageInfo().getRefToMessageId();
            result = refToMessageId;

            messagingDao.findMessageByMessageId(refToMessageId);
            result = messaging;

            messaging.getUserMessage();
            result = userMessage;

            domainConverter.convert(signalMessageLog, UIMessageEntity.class);
            result = uiMessageEntity;

            userMessageDefaultServiceHelper.getFinalRecipient(userMessage);
            result = finalRecipient;

            userMessageDefaultServiceHelper.getOriginalSender(userMessage);
            result = originalSender;

            uiMessageEntity.getLastModified();
            result = jmsTime;

        }};

        //tested method
        uiReplicationDataService.saveUIMessageFromSignalMessageLog(messageId, jmsTime.getTime());

        new FullVerifications(uiMessageEntity) {{

            uiMessageEntity.setEntityId(anyInt);

            String actualValue;
            uiMessageEntity.setMessageId(actualValue = withCapture());
            Assert.assertEquals(messageId, actualValue);

            uiMessageEntity.setConversationId(actualValue = withCapture());
            Assert.assertEquals(StringUtils.EMPTY, actualValue);

            uiMessageEntity.setFromId(actualValue = withCapture());
            Assert.assertEquals(fromPartyId, actualValue);

            uiMessageEntity.setToId(actualValue = withCapture());
            Assert.assertEquals(toPartyId, actualValue);

            uiMessageEntity.setFromScheme(actualValue = withCapture());
            Assert.assertEquals(originalSender, actualValue);

            uiMessageEntity.setToScheme(actualValue = withCapture());
            Assert.assertEquals(finalRecipient, actualValue);

            uiMessageEntity.setRefToMessageId(actualValue = withCapture());
            Assert.assertEquals(refToMessageId, actualValue);

            uiMessageEntity.setLastModified(withAny(new java.util.Date()));

            uiMessageEntity.setLastModified2(withAny(new java.util.Date()));

            final UIMessageEntity entityActual;
            uiMessageDao.create(entityActual = withCapture());
            Assert.assertNotNull(entityActual);

        }};
    }

    private UserMessageLogEntity createUserMessageLog() {
        UserMessageLogEntity userMessageLog = new UserMessageLogEntity();
        userMessageLog.setMessageId(messageId);
        userMessageLog.setMessageStatus(messageStatus);
        userMessageLog.setNotificationStatus(notificationStatus);
        userMessageLog.setMshRole(mshRole);
        userMessageLog.setMessageType(messageType);
        userMessageLog.setDeleted(deleted);
        userMessageLog.setReceived(received);
        userMessageLog.setSendAttempts(sendAttempts);
        userMessageLog.setSendAttemptsMax(sendAttemptsMax);
        userMessageLog.setNextAttempt(nextAttempt);
        userMessageLog.setFailed(failed);
        userMessageLog.setRestored(restored);
        userMessageLog.setMessageSubtype(messageSubtype);

        return userMessageLog;
    }

    private SignalMessageLog createSignalMessageLog() {
        SignalMessageLog signalMessageLog = new SignalMessageLog();
        signalMessageLog.setMessageId(messageId);
        signalMessageLog.setMessageStatus(messageStatus);
        signalMessageLog.setNotificationStatus(notificationStatus);
        signalMessageLog.setMshRole(mshRole);
        signalMessageLog.setMessageType(messageType);
        signalMessageLog.setDeleted(deleted);
        signalMessageLog.setReceived(received);
        signalMessageLog.setSendAttempts(sendAttempts);
        signalMessageLog.setSendAttemptsMax(sendAttemptsMax);
        signalMessageLog.setNextAttempt(nextAttempt);
        signalMessageLog.setFailed(failed);
        signalMessageLog.setRestored(restored);
        signalMessageLog.setMessageSubtype(messageSubtype);

        return signalMessageLog;
    }

    private UserMessage createUserMessage() {
        UserMessage userMessage = ebmsObjectFactory.createUserMessage();
        MessageInfo messageInfo = ebmsObjectFactory.createMessageInfo();
        messageInfo.setMessageId(messageId);
        messageInfo.setRefToMessageId(refToMessageId);
        CollaborationInfo collaborationInfo = ebmsObjectFactory.createCollaborationInfo();
        collaborationInfo.setConversationId(conversationId);

        userMessage.setMessageInfo(messageInfo);
        userMessage.setCollaborationInfo(collaborationInfo);
        userMessage.setPartyInfo(createPartyInfo());
        userMessage.setMessageProperties(createMessageProperties());

        return userMessage;
    }

    private SignalMessage createSignalMessage() {
        SignalMessage signalMessage = ebmsObjectFactory.createSignalMessage();
        MessageInfo messageInfo = ebmsObjectFactory.createMessageInfo();
        messageInfo.setMessageId(messageId);
        messageInfo.setRefToMessageId(refToMessageId);
        signalMessage.setMessageInfo(messageInfo);

        return signalMessage;
    }

    private PartyInfo createPartyInfo() {
        PartyInfo partyInfo = ebmsObjectFactory.createPartyInfo();

        PartyId partyId = ebmsObjectFactory.createPartyId();
        partyId.setValue(toPartyId);
        partyId.setType((toPartyIdType));

        To to = ebmsObjectFactory.createTo();
        to.getPartyId().add(partyId);
        partyInfo.setTo(to);

        partyId = ebmsObjectFactory.createPartyId();
        partyId.setValue(fromPartyId);
        partyId.setType((fromPartyIdType));

        From from = ebmsObjectFactory.createFrom();
        from.getPartyId().add(partyId);
        partyInfo.setFrom(from);

        return partyInfo;
    }

    private MessageProperties createMessageProperties() {
        Property finalRecipientProp = new Property();
        finalRecipientProp.setName(MessageConstants.FINAL_RECIPIENT);
        finalRecipientProp.setValue(finalRecipient);

        Property originalSenderProp = new Property();
        originalSenderProp.setName(MessageConstants.ORIGINAL_SENDER);
        originalSenderProp.setValue(originalSender);

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getProperty().add(finalRecipientProp);
        messageProperties.getProperty().add(originalSenderProp);

        return messageProperties;
    }
}