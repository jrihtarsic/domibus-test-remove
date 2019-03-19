package eu.domibus.plugin;

import eu.domibus.common.*;
import eu.domibus.ext.services.MessageExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.messaging.PModeMismatchException;
import eu.domibus.plugin.exception.TransformationException;
import eu.domibus.plugin.handler.MessagePuller;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for implementing plugins
 *
 * @author Christian Koch, Stefan Mueller
 */
public abstract class AbstractBackendConnector<U, T> implements BackendConnector<U, T> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractBackendConnector.class);

    private final String name;

    @Autowired
    protected MessageRetriever messageRetriever;

    @Autowired
    protected MessageSubmitter messageSubmitter;

    @Autowired
    protected MessagePuller messagePuller;

    @Autowired
    protected MessageExtService messageExtService;

    private MessageLister lister;

    public AbstractBackendConnector(final String name) {
        this.name = name;
    }

    public void setLister(final MessageLister lister) {
        this.lister = lister;
    }

    @Override
    // The following does not have effect at this level since the transaction would have already been rolled back!
    // @Transactional(noRollbackFor = {IllegalArgumentException.class, IllegalStateException.class})
    public String submit(final U message) throws MessagingProcessingException {
        try {
            final Submission messageData = getMessageSubmissionTransformer().transformToSubmission(message);
            return this.messageSubmitter.submit(messageData, this.getName());
        } catch (IllegalArgumentException iaEx) {
            throw new TransformationException(iaEx);
        } catch (IllegalStateException ise) {
            throw new PModeMismatchException(ise);
        }
    }



    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public T downloadMessage(final String messageId, final T target) throws MessageNotFoundException {
        T t = this.getMessageRetrievalTransformer().transformFromSubmission(this.messageRetriever.downloadMessage(messageId), target);
        lister.removeFromPending(messageId);
        return t;
    }


    @Override
    public Collection<String> listPendingMessages() {
        return lister.listPendingMessages();
    }

    @Override
    public MessageStatus getStatus(final String messageId) {
        return this.messageRetriever.getStatus(messageExtService.cleanMessageIdentifier(messageId));
    }

    @Override
    public List<ErrorResult> getErrorsForMessage(final String messageId) {
        return new ArrayList<>(this.messageRetriever.getErrorsForMessage(messageId));
    }

    @Override
    public void initiatePull(final String mpc) {
        messagePuller.initiatePull(mpc);
    }

    @Override
    public void messageReceiveFailed(MessageReceiveFailureEvent messageReceiveFailureEvent) {
        throw new UnsupportedOperationException("Plugins using " + Mode.PUSH.name() + " must implement this method");
    }

    @Override
    public void messageStatusChanged(MessageStatusChangeEvent event) {
        //this method should be implemented by the plugins needed to be notified when the User Message status changes
    }

    @Override
    public void deliverMessage(final String messageId) {
        throw new UnsupportedOperationException("Plugins using " + Mode.PUSH.name() + " must implement this method");
    }

    @Override
    public void messageSendSuccess(String messageId) {
        throw new UnsupportedOperationException("Plugins using " + Mode.PUSH.name() + " must implement this method");
    }

    @Override
    public void payloadSubmittedEvent(PayloadSubmittedEvent event) {
        //this method should be implemented by the plugins needed to be notified about payload submitted events
    }

    @Override
    public void payloadProcessedEvent(PayloadProcessedEvent event) {
        //this method should be implemented by the plugins needed to be notified about payload processed events
    }


    @Override
    public String getName() {
        return name;
    }

    protected String trim(String messageId) {
        return StringUtils.stripToEmpty(StringUtils.trimToEmpty(messageId));
    }
}
