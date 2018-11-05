package eu.domibus.ebms3.sender;

import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.message.attempt.MessageAttemptStatus;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.security.ChainCertificateInvalidException;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.common.services.ReliabilityService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.pki.PolicyService;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.Fault;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.Set;


/**
 * This class is responsible for the handling of outgoing messages.
 *
 * @author Christian Koch, Stefan Mueller
 * @since 3.0
 */
@Service(value = "messageSenderService")
public class MessageSender implements MessageListener {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageSender.class);

    private static final Set<MessageStatus> ALLOWED_STATUSES_FOR_SENDING = EnumSet.of(MessageStatus.SEND_ENQUEUED, MessageStatus.WAITING_FOR_RETRY);
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private UserMessageService userMessageService;

    @Autowired
    private MessagingDao messagingDao;


    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private MSHDispatcher mshDispatcher;

    @Autowired
    private EbMS3MessageBuilder messageBuilder;

    @Autowired
    private ReliabilityChecker reliabilityChecker;

    @Autowired
    private ResponseHandler responseHandler;

    @Autowired
    private RetryService retryService;

    @Autowired
    private MessageAttemptService messageAttemptService;

    @Autowired
    private MessageExchangeService messageExchangeService;

    @Autowired
    PolicyService policyService;

    @Autowired
    private ReliabilityService reliabilityService;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    @Autowired
    protected DomainContextProvider domainContextProvider;


    private void sendUserMessage(final String messageId, int retryCount) {
        final MessageStatus messageStatus = userMessageLogDao.getMessageStatus(messageId);

        if (MessageStatus.NOT_FOUND == messageStatus) {
            if (retryCount < MAX_RETRY_COUNT) {
                userMessageService.scheduleSending(messageId, retryCount + 1);
                LOG.warn("MessageStatus NOT_FOUND, retry count is [{}] -> reschedule sending", retryCount);
                return;
            }
            LOG.warn("Message [{}] has a status [{}] for [{}] times and will not be sent", messageId, MessageStatus.NOT_FOUND, retryCount);
            return;
        }

        if (!ALLOWED_STATUSES_FOR_SENDING.contains(messageStatus)) {
            LOG.warn("Message [{}] has a status [{}] which is not allowed for sending. Only the statuses [{}] are allowed", messageId, messageStatus, ALLOWED_STATUSES_FOR_SENDING);
            return;
        }


        LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_SEND_INITIATION);

        MessageAttempt attempt = new MessageAttempt();
        attempt.setMessageId(messageId);
        attempt.setStartDate(new Timestamp(System.currentTimeMillis()));
        MessageAttemptStatus attemptStatus = MessageAttemptStatus.SUCCESS;
        String attemptError = null;


        ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;
        // Assuming that everything goes fine
        ResponseHandler.CheckResult isOk = ResponseHandler.CheckResult.OK;

        LegConfiguration legConfiguration = null;
        final String pModeKey;

        final UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        try {
            pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            LOG.debug("PMode key found : " + pModeKey);
            legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
            LOG.info("Found leg [{}] for PMode key [{}]", legConfiguration.getName(), pModeKey);

            Policy policy;
            try {
                policy = policyService.parsePolicy("policies/" + legConfiguration.getSecurity().getPolicy());
            } catch (final ConfigurationException e) {

                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Policy configuration invalid", null, e);
                ex.setMshRole(MSHRole.SENDING);
                throw ex;
            }

            Party sendingParty = pModeProvider.getSenderParty(pModeKey);
            Validate.notNull(sendingParty, "Initiator party was not found");
            Party receiverParty = pModeProvider.getReceiverParty(pModeKey);
            Validate.notNull(receiverParty, "Responder party was not found");

            try {
                messageExchangeService.verifyReceiverCertificate(legConfiguration, receiverParty.getName());
                messageExchangeService.verifySenderCertificate(legConfiguration, sendingParty.getName());
            } catch (ChainCertificateInvalidException cciEx) {
                LOG.securityError(DomibusMessageCode.SEC_INVALID_X509CERTIFICATE, cciEx, null);
                attemptError = cciEx.getMessage();
                attemptStatus = MessageAttemptStatus.ABORT;
                // this flag is used in the finally clause
                reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.ABORT;
                LOG.error("Cannot handle request for message:[{}], Certificate is not valid or it has been revoked ", messageId, cciEx);
                LOG.info("Skipped checking the reliability for message [" + messageId + "]: message sending has been aborted");
                return;
            }

            LOG.debug("PMode found : " + pModeKey);
            final SOAPMessage soapMessage = messageBuilder.buildSOAPMessage(userMessage, legConfiguration);
            final SOAPMessage response = mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            /*isOk = responseHandler.handle(response);
            if (ResponseHandler.CheckResult.UNMARSHALL_ERROR.equals(isOk)) {
                EbMS3Exception e = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "Problem occurred during marshalling", messageId, null);
                e.setMshRole(MSHRole.SENDING);
                throw e;
            }
            reliabilityCheckSuccessful = reliabilityChecker.check(soapMessage, response, pModeKey);*/
        } catch (final SOAPFaultException soapFEx) {
            LOG.warn("Error for message with ID [" + messageId + "]", soapFEx);
        } catch (final EbMS3Exception e) {
            LOG.warn("Error for message with ID EbMS3Exception [" + messageId + "]", e);
        } catch (Throwable t) {
            LOG.error("Error sending message [{}]", messageId, t);
            throw t;
        } finally {

            LOG.info("-----------Finally: ");
        }
}

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 1200) // 20 minutes
    @MDCKey(DomibusLogger.MDC_MESSAGE_ID)
    public void onMessage(final Message message) {
        LOG.debug("Processing message [{}]", message);
        Long delay;
        String messageId = null;
        int retryCount = 0;
        try {
            messageId = message.getStringProperty(MessageConstants.MESSAGE_ID);
            if (message.propertyExists(MessageConstants.RETRY_COUNT)) {
                retryCount = message.getIntProperty(MessageConstants.RETRY_COUNT);
            }
            final String domainCode = message.getStringProperty(MessageConstants.DOMAIN);
            LOG.debug("Sending message ID [{}] for domain [{}]", messageId, domainCode);
            domainContextProvider.setCurrentDomain(domainCode);
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);
            delay = message.getLongProperty(MessageConstants.DELAY);
            if (delay > 0) {
                userMessageService.scheduleSending(messageId, delay);
                return;
            }
        } catch (final NumberFormatException nfe) {
            //This is ok, no delay has been set
        } catch (final JMSException e) {
            LOG.error("Error processing message", e);
        }
        sendUserMessage(messageId, retryCount);
    }

}
