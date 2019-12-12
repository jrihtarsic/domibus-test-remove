package eu.domibus.ebms3.sender;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.ErrorLogDao;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.SignalMessageDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.metrics.MetricsHelper;
import eu.domibus.common.model.logging.ErrorLogEntry;
import eu.domibus.common.services.ReliabilityService;
import eu.domibus.common.services.impl.AS4ReceiptService;
import eu.domibus.core.message.SignalMessageLogDefaultService;
import eu.domibus.core.nonrepudiation.NonRepudiationService;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.ebms3.common.model.Error;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * @author Christian Koch, Stefan Mueller, Federico Martini
 * @author Cosmin Baciu
 */
@Service
public class ResponseHandler {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(ResponseHandler.class);

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private ErrorLogDao errorLogDao;

    @Autowired
    private SignalMessageDao signalMessageDao;

    @Autowired
    private NonRepudiationService nonRepudiationService;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private SignalMessageLogDefaultService signalMessageLogDefaultService;

    @Autowired
    private UIReplicationSignalService uiReplicationSignalService;

    @Autowired
    protected ReliabilityService reliabilityService;

    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseResult verifyResponse(final SOAPMessage response) throws EbMS3Exception {
        LOGGER.debug("Verifying response");

        ResponseResult result = new ResponseResult();

        final Messaging messaging;
        try {
            messaging = messageUtil.getMessagingWithDom(response);
            result.setResponseMessaging(messaging);
        } catch (SOAPException ex) {
            LOGGER.error("Error while un-marshalling message", ex);
            result.setResponseStatus(ResponseStatus.UNMARSHALL_ERROR);
            return result;
        }

        final SignalMessage signalMessage = messaging.getSignalMessage();
        final ResponseStatus responseStatus = getResponseStatus(signalMessage);
        result.setResponseStatus(responseStatus);

        return result;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void saveResponse(final SOAPMessage response, final Messaging sentMessage, final Messaging messagingResponse) {
        final SignalMessage signalMessage = messagingResponse.getSignalMessage();
        Timer.Context responseHandlerContext = null;
        try {
            responseHandlerContext = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(ResponseHandler.class, "nonrepudiation.saveResponse")).time();
            nonRepudiationService.saveResponse(response, signalMessage);
        } finally {
            if (responseHandlerContext != null) {
                responseHandlerContext.stop();
            }
        }

        responseHandlerContext = null;
        try {
            responseHandlerContext = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(ResponseHandler.class, "nonrepudiation.saveResponse")).time();
            // Stores the signal message
            signalMessageDao.create(signalMessage);

            sentMessage.setSignalMessage(signalMessage);
            messagingDao.update(sentMessage);

        } finally {
            if (responseHandlerContext != null) {
                responseHandlerContext.stop();
            }
        }

        responseHandlerContext = null;
        try {
            responseHandlerContext = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(ResponseHandler.class, "nonrepudiation.signalMessageLog")).time();
            // Builds the signal message log
            // Updating the reference to the signal message
            String userMessageService = sentMessage.getUserMessage().getCollaborationInfo().getService().getValue();
            String userMessageAction = sentMessage.getUserMessage().getCollaborationInfo().getAction();

            signalMessageLogDefaultService.save(signalMessage.getMessageInfo().getMessageId(), userMessageService, userMessageAction);

            createWarningEntries(signalMessage);
        } finally {
            if (responseHandlerContext != null) {
                responseHandlerContext.stop();
            }
        }

        responseHandlerContext = null;
        try {
            responseHandlerContext = MetricsHelper.getMetricRegistry().timer(MetricRegistry.name(ResponseHandler.class, "nonrepudiation.uiReplication")).time();
        //UI replication
        uiReplicationSignalService.signalMessageReceived(signalMessage.getMessageInfo().getMessageId());
        } finally {
            if (responseHandlerContext != null) {
                responseHandlerContext.stop();
            }
        }
    }

    protected void createWarningEntries(SignalMessage signalMessage) {
        if (signalMessage.getError() == null || signalMessage.getError().isEmpty()) {
            LOGGER.debug("No warning entries to create");
            return;
        }

        LOGGER.debug("Creating warning entries");

        for (final Error error : signalMessage.getError()) {
            if (ErrorCode.SEVERITY_WARNING.equalsIgnoreCase(error.getSeverity())) {
                final String errorCode = error.getErrorCode();
                final String errorDetail = error.getErrorDetail();
                final String refToMessageInError = error.getRefToMessageInError();

                LOGGER.warn("Creating warning error with error code [{}], error detail [{}] and refToMessageInError [{}]", errorCode, errorDetail, refToMessageInError);

                EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.findErrorCodeBy(errorCode), errorDetail, refToMessageInError, null);
                final ErrorLogEntry errorLogEntry = new ErrorLogEntry(ebMS3Ex);
                this.errorLogDao.create(errorLogEntry);
            }
        }
    }

    protected ResponseStatus getResponseStatus(SignalMessage signalMessage) throws EbMS3Exception {
        LOGGER.debug("Getting response status");

        // Checks if the signal message is Ok
        if (signalMessage.getError() == null || signalMessage.getError().isEmpty()) {
            LOGGER.debug("Response message contains no errors");
            return ResponseStatus.OK;
        }

        for (final Error error : signalMessage.getError()) {
            if (ErrorCode.SEVERITY_FAILURE.equalsIgnoreCase(error.getSeverity())) {
                EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.findErrorCodeBy(error.getErrorCode()), error.getErrorDetail(), error.getRefToMessageInError(), null);
                ebMS3Ex.setMshRole(MSHRole.SENDING);
                throw ebMS3Ex;
            }
        }

        return ResponseStatus.WARNING;
    }


    public enum ResponseStatus {
        OK, WARNING, UNMARSHALL_ERROR
    }

}
