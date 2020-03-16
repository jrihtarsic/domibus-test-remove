package eu.domibus.ebms3.receiver.handler;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.core.message.MessagingDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.exception.EbMS3Exception;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Reliability;
import eu.domibus.common.model.configuration.ReplyPattern;
import eu.domibus.core.message.UserMessageLog;
import eu.domibus.core.message.reliability.ReliabilityService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.message.reliability.ReliabilityChecker;
import eu.domibus.ebms3.sender.ResponseHandler;
import eu.domibus.ebms3.sender.ResponseResult;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;

import static eu.domibus.core.metrics.MetricNames.INCOMING_USER_MESSAGE_RECEIPT;

/**
 * Handles the incoming AS4 receipts
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class IncomingUserMessageReceiptHandler implements IncomingMessageHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(IncomingUserMessageReceiptHandler.class);

    protected Reliability sourceMessageReliability;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private EbMS3MessageBuilder messageBuilder;

    @Autowired
    private ResponseHandler responseHandler;

    @Autowired
    private ReliabilityChecker reliabilityChecker;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    protected ReliabilityService reliabilityService;

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    protected SoapUtil soapUtil;

    @Transactional
    @Override
    @Timer(INCOMING_USER_MESSAGE_RECEIPT)
    @Counter(INCOMING_USER_MESSAGE_RECEIPT)
    public SOAPMessage processMessage(SOAPMessage request, Messaging messaging) {
        LOG.debug("Processing UserMessage receipt");
        final SOAPMessage soapMessage = handleUserMessageReceipt(request, messaging);
        return soapMessage;
    }

    protected SOAPMessage handleUserMessageReceipt(SOAPMessage request, Messaging messaging) {
        String messageId = messaging.getSignalMessage().getMessageInfo().getRefToMessageId();

        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        if (MessageStatus.ACKNOWLEDGED == userMessageLog.getMessageStatus()) {
            LOG.error("Received a UserMessage receipt for an already acknowledged message with status [{}]", userMessageLog.getMessageStatus());
            EbMS3Exception ebMS3Exception = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, String.format("UserMessage with ID [%s] is already acknowledged", messageId), messageId, null);
            return messageBuilder.getSoapMessage(ebMS3Exception);
        }

        ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.ABORT;
        ResponseResult responseResult = null;
        LegConfiguration legConfiguration = null;
        UserMessage userMessage = null;
        Messaging sentMessage = null;
        try {
            sentMessage = messagingDao.findMessageByMessageId(messageId);
            userMessage = sentMessage.getUserMessage();
            String pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            LOG.debug("PMode key found : " + pModeKey);

            legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
            LOG.debug("Found leg [{}] for PMode key [{}]", legConfiguration.getName(), pModeKey);

            SOAPMessage soapMessage = getSoapMessage(legConfiguration, userMessage);
            responseResult = responseHandler.verifyResponse(request);

            if (ResponseHandler.ResponseStatus.UNMARSHALL_ERROR.equals(responseResult.getResponseStatus())) {
                EbMS3Exception e = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "Problem occurred during marshalling", messageId, null);
                e.setMshRole(MSHRole.SENDING);
                throw e;
            }
            reliabilityCheckSuccessful = reliabilityChecker.check(soapMessage, request, responseResult, getSourceMessageReliability());
        } catch (final SOAPFaultException soapFEx) {
            if (soapFEx.getCause() instanceof Fault && soapFEx.getCause().getCause() instanceof EbMS3Exception) {
                reliabilityChecker.handleEbms3Exception((EbMS3Exception) soapFEx.getCause().getCause(), messageId);
            } else {
                LOG.warn("Error for message with ID [" + messageId + "]", soapFEx);
            }
        } catch (final EbMS3Exception e) {
            reliabilityChecker.handleEbms3Exception(e, messageId);
        } finally {
            reliabilityService.handleReliability(messageId, sentMessage, userMessageLog, reliabilityCheckSuccessful, request, responseResult, legConfiguration, null);
        }
        return null;
    }

    protected SOAPMessage getSoapMessage(LegConfiguration legConfiguration, UserMessage userMessage) throws EbMS3Exception {
        return messageBuilder.buildSOAPMessage(userMessage, legConfiguration);
    }

    protected Reliability getSourceMessageReliability() {
        if (sourceMessageReliability == null) {
            sourceMessageReliability = new Reliability();
            sourceMessageReliability.setNonRepudiation(false);
            sourceMessageReliability.setReplyPattern(ReplyPattern.RESPONSE);
        }
        return sourceMessageReliability;
    }

}
