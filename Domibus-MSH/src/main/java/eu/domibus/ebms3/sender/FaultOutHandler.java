package eu.domibus.ebms3.sender;

import com.codahale.metrics.Timer;
import eu.domibus.common.MSHRole;
import eu.domibus.common.dao.ErrorLogDao;
import eu.domibus.api.metrics.MetricsHelper;
import eu.domibus.common.model.logging.ErrorLogEntry;
import eu.domibus.ebms3.common.handler.AbstractFaultHandler;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Set;

/**
 * This handler is responsible for processing of incoming ebMS3 errors as a response of an outgoing ebMS3 message.
 *
 * @author Christian Koch, Stefan Mueller
 */
public class FaultOutHandler extends AbstractFaultHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FaultOutHandler.class);

    @Autowired
    private ErrorLogDao errorLogDao;

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(final SOAPMessageContext context) {
        //Do nothing as this is a fault handler
        return true;
    }


    /**
     * The {@code handleFault} method is responsible for logging of incoming ebMS3 errors
     */
    @Override
    public boolean handleFault(final SOAPMessageContext context) {
        Timer.Context timerContext = null;
        try {
            timerContext = MetricsHelper.getMetricRegistry().timer("FaultOutHandler.handleFault").time();
        final Messaging messaging = this.extractMessaging(context.getMessage());

        FaultOutHandler.LOG.debug("An ebMS3 error was received for message with ebMS3 messageId:" + messaging.getSignalMessage().getMessageInfo().getMessageId() + ". Please check the database for more detailed information.");
        this.errorLogDao.create(ErrorLogEntry.parse(messaging, MSHRole.SENDING));

        return true;
        }finally {
            if (timerContext != null) {
                timerContext.stop();
            }
        }
    }

    @Override
    public void close(final MessageContext context) {

    }
}
