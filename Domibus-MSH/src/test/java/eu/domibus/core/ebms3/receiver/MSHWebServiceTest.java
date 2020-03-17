package eu.domibus.core.ebms3.receiver;

import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandler;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandlerFactory;
import eu.domibus.core.ebms3.sender.client.DispatchClientDefaultProvider;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;

/**
 * @author Arun Raj
 * @author Cosmin Baciu
 * @since 3.3
 */

@RunWith(JMockit.class)
public class MSHWebServiceTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MSHWebServiceTest.class);
    private static final String VALID_PMODE_CONFIG_URI = "samplePModes/domibus-configuration-valid.xml";

    @Tested
    MSHWebservice mshWebservice;

    @Injectable
    MessageUtil messageUtil;

    @Injectable
    IncomingMessageHandlerFactory incomingMessageHandlerFactory;

    @Test
    public void testInvokeHappyFlow(@Injectable SOAPMessage request,
                                    @Injectable Messaging messaging,
                                    @Injectable IncomingMessageHandler messageHandler,
                                    @Mocked PhaseInterceptorChain interceptors) throws EbMS3Exception {
        new Expectations() {{
            PhaseInterceptorChain.getCurrentMessage().get(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY);
            result = messaging;

            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = messageHandler;
        }};

        mshWebservice.invoke(request);

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
        }};
    }

    @Test(expected = WebServiceException.class)
    public void testInvokeNoHandlerFound(@Injectable SOAPMessage request,
                                         @Injectable Messaging messaging,
                                         @Injectable IncomingMessageHandler messageHandler,
                                         @Mocked PhaseInterceptorChain interceptors) throws EbMS3Exception {
        new Expectations() {{
            PhaseInterceptorChain.getCurrentMessage().get(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY);
            result = messaging;

            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = null;
        }};

        mshWebservice.invoke(request);

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
            times = 0;
        }};
    }
}
