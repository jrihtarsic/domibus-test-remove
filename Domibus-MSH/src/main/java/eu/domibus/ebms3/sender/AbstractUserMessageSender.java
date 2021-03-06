package eu.domibus.ebms3.sender;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.message.attempt.MessageAttemptStatus;
import eu.domibus.api.security.ChainCertificateInvalidException;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.metrics.Counter;
import eu.domibus.common.metrics.Timer;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.common.services.ReliabilityService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.pki.PolicyService;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.interceptor.Fault;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;
import java.sql.Timestamp;

import static eu.domibus.common.metrics.MetricNames.OUTGOING_USER_MESSAGE;

/**
 * Common logic for sending AS4 messages to C3
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
public abstract class AbstractUserMessageSender implements MessageSender {

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected MSHDispatcher mshDispatcher;

    @Autowired
    protected EbMS3MessageBuilder messageBuilder;

    @Autowired
    protected ReliabilityChecker reliabilityChecker;

    @Autowired
    protected ResponseHandler responseHandler;

    @Autowired
    protected MessageAttemptService messageAttemptService;

    @Autowired
    protected MessageExchangeService messageExchangeService;

    @Autowired
    protected PolicyService policyService;

    @Autowired
    protected ReliabilityService reliabilityService;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Override
    @Timer(OUTGOING_USER_MESSAGE)
    @Counter(OUTGOING_USER_MESSAGE)
    public void sendMessage(final UserMessage userMessage) {
        String messageId = userMessage.getMessageInfo().getMessageId();

        MessageAttempt attempt = new MessageAttempt();
        attempt.setMessageId(messageId);
        attempt.setStartDate(new Timestamp(System.currentTimeMillis()));
        attempt.setStatus(MessageAttemptStatus.SUCCESS);

        ReliabilityChecker.CheckResult reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;
        // Assuming that everything goes fine
        ResponseHandler.CheckResult isOk = ResponseHandler.CheckResult.OK;

        LegConfiguration legConfiguration = null;
        final String pModeKey;

        try {

            try {
                validateBeforeSending(userMessage);
            } catch (DomibusCoreException e) {
                getLog().error("Validation exception: message [{}] will not be send", messageId, e);
                attempt.setError(e.getMessage());
                attempt.setStatus(MessageAttemptStatus.ABORT);
                // this flag is used in the finally clause
                reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.ABORT;
                return;
            }


            pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
            getLog().debug("PMode key found : " + pModeKey);
            legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
            getLog().info("Found leg [{}] for PMode key [{}]", legConfiguration.getName(), pModeKey);

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
                getLog().securityError(DomibusMessageCode.SEC_INVALID_X509CERTIFICATE, cciEx);
                attempt.setError(cciEx.getMessage());
                attempt.setStatus(MessageAttemptStatus.ERROR);
                // this flag is used in the finally clause
                reliabilityCheckSuccessful = ReliabilityChecker.CheckResult.SEND_FAIL;
                getLog().error("Cannot handle request for message:[{}], Certificate is not valid or it has been revoked ", messageId, cciEx);
                return;
            }

            getLog().debug("PMode found : " + pModeKey);
            final SOAPMessage soapMessage = createSOAPMessage(userMessage, legConfiguration);
            final SOAPMessage response = mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            isOk = responseHandler.handle(response);
            if (ResponseHandler.CheckResult.UNMARSHALL_ERROR.equals(isOk)) {
                EbMS3Exception e = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "Problem occurred during marshalling", messageId, null);
                e.setMshRole(MSHRole.SENDING);
                throw e;
            }
            reliabilityCheckSuccessful = reliabilityChecker.check(soapMessage, response, pModeKey);
        } catch (final SOAPFaultException soapFEx) {
            getLog().error("A SOAP fault occurred when sending message with ID [{}]", messageId, soapFEx);

            if (soapFEx.getCause() instanceof Fault && soapFEx.getCause().getCause() instanceof EbMS3Exception) {
                reliabilityChecker.handleEbms3Exception((EbMS3Exception) soapFEx.getCause().getCause(), messageId);
            }
            attempt.setError(soapFEx.getMessage());
            attempt.setStatus(MessageAttemptStatus.ERROR);
        } catch (final EbMS3Exception e) {
            getLog().error("EbMS3 exception occurred when sending message with ID [{}]", messageId, e);

            reliabilityChecker.handleEbms3Exception(e, messageId);
            attempt.setError(e.getMessage());
            attempt.setStatus(MessageAttemptStatus.ERROR);
        } catch (Throwable t) {
            //NOSONAR: Catching Throwable is done on purpose in order to even catch out of memory exceptions in case large files are sent.
            getLog().error("Error occurred when sending message with ID [{}]", messageId, t);

            attempt.setError(t.getMessage());
            attempt.setStatus(MessageAttemptStatus.ERROR);
            throw t;
        } finally {
            try {
                getLog().debug("Finally handle reliability");
                reliabilityService.handleReliability(messageId, userMessage, reliabilityCheckSuccessful, isOk, legConfiguration);
                updateAndCreateAttempt(attempt);
            } catch (Exception ex) {
                getLog().warn("Finally exception when handlingReliability", ex);
                reliabilityService.handleReliabilityInNewTransaction(messageId, userMessage, reliabilityCheckSuccessful, isOk, legConfiguration);
                updateAndCreateAttempt(attempt);
            }
        }
    }

    protected void validateBeforeSending(final UserMessage userMessage) {
        //can be overridden by child implementations
    }

    protected abstract SOAPMessage createSOAPMessage(final UserMessage userMessage, LegConfiguration legConfiguration) throws EbMS3Exception;

    protected abstract DomibusLogger getLog();

    protected void updateAndCreateAttempt(MessageAttempt attempt) {
        attempt.setEndDate(new Timestamp(System.currentTimeMillis()));
        messageAttemptService.create(attempt);
    }


}
