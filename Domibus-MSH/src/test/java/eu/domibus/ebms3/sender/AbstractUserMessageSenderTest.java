package eu.domibus.ebms3.sender;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.message.attempt.MessageAttemptStatus;
import eu.domibus.api.security.ChainCertificateInvalidException;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.message.UserMessageLog;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.reliability.ReliabilityService;
import eu.domibus.core.message.reliability.ReliabilityChecker;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.pki.PolicyService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.neethi.Policy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.soap.SOAPMessage;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@RunWith(JMockit.class)
public class AbstractUserMessageSenderTest {


    @Tested
    AbstractUserMessageSender abstractUserMessageSender;

    @Injectable
    protected PModeProvider pModeProvider;

    @Injectable
    protected MSHDispatcher mshDispatcher;

    @Injectable
    protected EbMS3MessageBuilder messageBuilder;

    @Injectable
    protected ReliabilityChecker reliabilityChecker;

    @Injectable
    protected ResponseHandler responseHandler;

    @Injectable
    protected MessageAttemptService messageAttemptService;

    @Injectable
    protected MessageExchangeService messageExchangeService;

    @Injectable
    protected PolicyService policyService;

    @Injectable
    protected ReliabilityService reliabilityService;

    @Injectable
    protected UserMessageLogDao userMessageLogDao;


    private final String messageId = UUID.randomUUID().toString();

    private final String senderName = "domibus-blue";
    private final String receiverName = "domibus-red";
    private final String legConfigurationName = "pushTestcase1tc1Action";
    private final String pModeKey = "toto";
    private final String configPolicy = "tototiti";
    static final String POLICIES = "policies/";


    @Test
    public void testSendMessage(@Injectable Messaging messaging,
                                @Injectable UserMessage userMessage,
                                @Injectable UserMessageLog userMessageLog,
                                @Injectable LegConfiguration legConfiguration,
                                @Injectable Policy policy,
                                @Injectable Party senderParty,
                                @Injectable Party receiverParty,
                                @Injectable SOAPMessage soapMessage,
                                @Injectable SOAPMessage response,
                                @Injectable ResponseResult responseResult) throws Exception {

        final ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;
        MessageAttemptStatus attemptStatus = MessageAttemptStatus.SUCCESS;
        String attemptError = null;
        String messageId = "123";

        new Expectations(abstractUserMessageSender) {{
            messaging.getUserMessage();
            result = userMessage;

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            abstractUserMessageSender.getLog();
            result = DomibusLoggerFactory.getLogger(AbstractUserMessageSenderTest.class);

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            legConfiguration.getName();
            result = legConfigurationName;

            legConfiguration.getSecurity().getPolicy();
            result = configPolicy;

            policyService.parsePolicy(POLICIES + legConfiguration.getSecurity().getPolicy());
            result = policy;

            pModeProvider.getSenderParty(pModeKey);
            result = senderParty;

            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            receiverParty.getName();
            result = receiverName;

            senderParty.getName();
            result = senderName;

            abstractUserMessageSender.createSOAPMessage(userMessage, legConfiguration);
            result = soapMessage;

            mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            result = response;

            responseHandler.verifyResponse(response);
            result = responseResult;

            responseResult.getResponseStatus();
            result = ResponseHandler.ResponseStatus.OK;

            reliabilityChecker.check(soapMessage, response, responseResult, legConfiguration);
            result = reliabilityCheckSuccessful;

        }};

        //tested method
        abstractUserMessageSender.sendMessage(messaging, userMessageLog);

        new FullVerifications(abstractUserMessageSender) {{
            LegConfiguration legConfigurationActual;
            String receiverPartyNameActual;
            messageExchangeService.verifyReceiverCertificate(legConfigurationActual = withCapture(), receiverPartyNameActual = withCapture());
            Assert.assertEquals(legConfiguration.getName(), legConfigurationActual.getName());
            Assert.assertEquals(receiverName, receiverPartyNameActual);


            String senderPartyNameActual;
            messageExchangeService.verifySenderCertificate(legConfigurationActual = withCapture(), senderPartyNameActual = withCapture());
            Assert.assertEquals(legConfiguration.getName(), legConfigurationActual.getName());
            Assert.assertEquals(senderName, senderPartyNameActual);

            String messageIdActual;
            ReliabilityChecker.CheckResult checkResultActual;

            reliabilityService.handleReliability(messageIdActual = withCapture(), messaging, userMessageLog, checkResultActual = withCapture(), response, responseResult, legConfiguration, null);
            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(reliabilityCheckSuccessful, checkResultActual);

        }};
    }

    @Test
    public void testSendMessage_WrongPolicyConfig_Exception(@Mocked final Messaging messaging,
                                                            @Mocked final UserMessage userMessage,
                                                            @Mocked final UserMessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration) throws EbMS3Exception {

        final MessageAttempt attempt = createMessageAttempt();
        final MessageAttemptStatus attemptStatus = MessageAttemptStatus.ERROR;
        final String attemptError = "Policy configuration invalid";
        final ConfigurationException configurationException = new ConfigurationException("policy file not found");

        new Expectations(abstractUserMessageSender) {{
            messaging.getUserMessage();
            result = userMessage;

            abstractUserMessageSender.getLog();
            result = DomibusLoggerFactory.getLogger(AbstractUserMessageSenderTest.class);

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            legConfiguration.getName();
            result = legConfigurationName;

            legConfiguration.getSecurity().getPolicy();
            result = configPolicy;

            policyService.parsePolicy(POLICIES + legConfiguration.getSecurity().getPolicy());
            result = configurationException;

        }};

        //tested method
        abstractUserMessageSender.sendMessage(messaging, userMessageLog);

        new FullVerifications(abstractUserMessageSender) {{
            EbMS3Exception ebMS3ExceptionActual;
            reliabilityChecker.handleEbms3Exception(ebMS3ExceptionActual = withCapture(), messageId);
            Assert.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0010, ebMS3ExceptionActual.getErrorCode());
            Assert.assertEquals("Policy configuration invalid", ebMS3ExceptionActual.getErrorDetail());
            Assert.assertEquals(MSHRole.SENDING, ebMS3ExceptionActual.getMshRole());
        }};
    }

    @Test
    public void testSendMessage_ChainCertificateInvalid_Exception(@Mocked final Messaging messaging,
                                                                  @Mocked final UserMessage userMessage, @Mocked final UserMessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration,
                                                                  final @Mocked Party senderParty, final @Mocked Party receiverParty, @Mocked SOAPMessage response) throws Exception {
        final MessageAttempt attempt = createMessageAttempt();
        final MessageAttemptStatus attemptStatus = MessageAttemptStatus.ERROR;
        final String chainExceptionMessage = "certificate invalid";
        final String attemptError = "[" + DomibusErrorCode.DOM_001 + "]:" + chainExceptionMessage;
        final ChainCertificateInvalidException chainCertificateInvalidException = new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, chainExceptionMessage);
        final ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;


        new Expectations(abstractUserMessageSender) {{
            messaging.getUserMessage();
            result = userMessage;

            abstractUserMessageSender.getLog();
            result = DomibusLoggerFactory.getLogger(AbstractUserMessageSenderTest.class);

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            legConfiguration.getName();
            result = legConfigurationName;

            legConfiguration.getSecurity().getPolicy();
            result = configPolicy;

            pModeProvider.getSenderParty(pModeKey);
            result = senderParty;

            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            receiverParty.getName();
            result = receiverName;

            messageExchangeService.verifyReceiverCertificate(legConfiguration, receiverParty.getName());
            result = chainCertificateInvalidException;
        }};

        //tested method
        abstractUserMessageSender.sendMessage(messaging, userMessageLog);

        new FullVerifications(abstractUserMessageSender) {{
            String messageIdActual;
            ReliabilityChecker.CheckResult checkResultActual;
            reliabilityService.handleReliability(messageIdActual = withCapture(), messaging, userMessageLog, checkResultActual = withCapture(), null, null, legConfiguration, null);
            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(reliabilityCheckSuccessful, checkResultActual);

        }};
    }


    @Test
    public void testSendMessage_UnmarshallingError_Exception(final @Mocked Messaging messaging,
                                                             @Mocked final UserMessage userMessage, @Mocked final UserMessageLog userMessageLog, @Mocked final LegConfiguration legConfiguration,
                                                             @Mocked final Policy policy, final @Mocked Party senderParty, final @Mocked Party receiverParty,
                                                             final @Mocked SOAPMessage soapMessage, final @Mocked SOAPMessage response,
                                                             @Injectable ResponseResult responseResult) throws Exception {

        final MessageAttemptStatus attemptStatus = MessageAttemptStatus.ERROR;
        final String attemptError = "Problem occurred during marshalling";
        final ResponseHandler.ResponseStatus isOk = ResponseHandler.ResponseStatus.UNMARSHALL_ERROR;
        final ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;

        new Expectations(abstractUserMessageSender) {{
            messaging.getUserMessage();
            result = userMessage;

            responseResult.getResponseStatus();
            result  = ResponseHandler.ResponseStatus.UNMARSHALL_ERROR;

            abstractUserMessageSender.getLog();
            result = DomibusLoggerFactory.getLogger(AbstractUserMessageSenderTest.class);

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            legConfiguration.getName();
            result = legConfigurationName;

            legConfiguration.getSecurity().getPolicy();
            result = configPolicy;

            policyService.parsePolicy(POLICIES + legConfiguration.getSecurity().getPolicy());
            result = policy;

            pModeProvider.getSenderParty(pModeKey);
            result = senderParty;

            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            receiverParty.getName();
            result = receiverName;

            senderParty.getName();
            result = senderName;

            abstractUserMessageSender.createSOAPMessage(userMessage, legConfiguration);
            result = soapMessage;

            mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            result = response;

            responseHandler.verifyResponse(response);
            result = responseResult;
        }};

        //tested method
        abstractUserMessageSender.sendMessage(messaging, userMessageLog);

        new FullVerifications(abstractUserMessageSender, messageExchangeService, reliabilityChecker) {{
            LegConfiguration legConfigurationActual;
            String receiverPartyNameActual;
            messageExchangeService.verifyReceiverCertificate(legConfigurationActual = withCapture(), receiverPartyNameActual = withCapture());
            Assert.assertEquals(legConfiguration.getName(), legConfigurationActual.getName());
            Assert.assertEquals(receiverName, receiverPartyNameActual);


            String senderPartyNameActual;
            messageExchangeService.verifySenderCertificate(legConfigurationActual = withCapture(), senderPartyNameActual = withCapture());
            Assert.assertEquals(legConfiguration.getName(), legConfigurationActual.getName());
            Assert.assertEquals(senderName, senderPartyNameActual);

            EbMS3Exception ebMS3ExceptionActual;
            reliabilityChecker.handleEbms3Exception(ebMS3ExceptionActual = withCapture(), messageId);
            Assert.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0004, ebMS3ExceptionActual.getErrorCode());
            Assert.assertEquals("Problem occurred during marshalling", ebMS3ExceptionActual.getErrorDetail());
            Assert.assertEquals(MSHRole.SENDING, ebMS3ExceptionActual.getMshRole());

            String messageIdActual;
            ReliabilityChecker.CheckResult checkResultActual;
            reliabilityService.handleReliability(messageIdActual = withCapture(), messaging, userMessageLog, checkResultActual = withCapture(), response, responseResult, legConfiguration, null);
            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(reliabilityCheckSuccessful, checkResultActual);

        }};
    }

    @Test
    public void testSendMessage_DispatchError_Exception(final @Injectable Messaging messaging,
                                                        @Injectable final UserMessage userMessage,
                                                        @Injectable final UserMessageLog userMessageLog,
                                                        @Injectable final LegConfiguration legConfiguration,
                                                        @Injectable final Policy policy,
                                                        final @Injectable Party senderParty,
                                                        final @Injectable Party receiverParty,
                                                        final @Injectable SOAPMessage soapMessage,
                                                        @Injectable SOAPMessage response,
                                                        @Injectable ResponseResult responseResult) throws Exception {

        final ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;

        String attemptError = "OutOfMemory occurred while dispatching messages";

        new Expectations(abstractUserMessageSender) {{
            messaging.getUserMessage();
            result = userMessage;

            userMessage.getMessageInfo().getMessageId();
            result = messageId;

            abstractUserMessageSender.getLog();
            result = DomibusLoggerFactory.getLogger(AbstractUserMessageSenderTest.class);

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            legConfiguration.getName();
            result = legConfigurationName;

            legConfiguration.getSecurity().getPolicy();
            result = configPolicy;

            policyService.parsePolicy(POLICIES + legConfiguration.getSecurity().getPolicy());
            result = policy;

            pModeProvider.getSenderParty(pModeKey);
            result = senderParty;

            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            receiverParty.getName();
            result = receiverName;

            senderParty.getName();
            result = senderName;

            abstractUserMessageSender.createSOAPMessage(userMessage, legConfiguration);
            result = soapMessage;

            mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            result = new OutOfMemoryError(attemptError);

        }};

        try {
            //tested method
            abstractUserMessageSender.sendMessage(messaging, userMessageLog);
            Assert.fail("exception expected");
        } catch (Throwable t) {
            Assert.assertTrue(t instanceof OutOfMemoryError);
        }

        new Verifications() {{
            String messageIdActual;
            ReliabilityChecker.CheckResult checkResultActual;
            reliabilityService.handleReliability(messageIdActual = withCapture(), messaging, userMessageLog, checkResultActual = withCapture(), null, null, legConfiguration, null);
            Assert.assertEquals(messageId, messageIdActual);
            Assert.assertEquals(reliabilityCheckSuccessful, checkResultActual);
        }};
    }


    protected MessageAttempt createMessageAttempt() {
        MessageAttempt attempt = new MessageAttempt();
        attempt.setMessageId(messageId);
        attempt.setStartDate(new Timestamp(System.currentTimeMillis()));
        return attempt;
    }

}