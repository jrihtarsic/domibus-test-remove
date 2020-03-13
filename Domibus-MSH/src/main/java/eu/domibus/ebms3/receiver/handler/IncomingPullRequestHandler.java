package eu.domibus.ebms3.receiver.handler;

import eu.domibus.api.pmode.PModeException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.core.pull.PullContext;
import eu.domibus.core.security.AuthorizationService;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.PullRequest;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.soap.SOAPMessage;

import static eu.domibus.core.metrics.MetricNames.INCOMING_PULL_REQUEST;

/**
 * Handles the incoming AS4 pull request
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class IncomingPullRequestHandler implements IncomingMessageHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(IncomingPullRequestHandler.class);

    @Autowired
    private PullRequestHandler pullRequestHandler;

    @Autowired
    private MessageExchangeService messageExchangeService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    @Timer(INCOMING_PULL_REQUEST)
    @Counter(INCOMING_PULL_REQUEST)
    public SOAPMessage processMessage(SOAPMessage request, Messaging messaging) throws EbMS3Exception {
        authorizationService.authorizePullRequest(request, messaging.getSignalMessage().getPullRequest());
        LOG.trace("before pull request.");
        final SOAPMessage soapMessage = handlePullRequest(messaging);
        LOG.trace("returning pull request message.");
        return soapMessage;
    }

    protected SOAPMessage handlePullRequest(Messaging messaging) {
        PullRequest pullRequest = messaging.getSignalMessage().getPullRequest();
        PullContext pullContext;
        String mpc = pullRequest.getMpc();
        try {
            LOG.debug("Extract process on MPC [{}]", mpc);
            pullContext = messageExchangeService.extractProcessOnMpc(mpc);
        } catch (PModeException pme) {
            LOG.debug("No process for mpc [{}]", mpc);
            if (messageExchangeService.forcePullOnMpc(pullRequest.getMpc())) {
                LOG.debug("Extract base mpc");
                mpc = messageExchangeService.extractBaseMpc(pullRequest.getMpc());
                LOG.debug("Trying base mpc [{}]", mpc);
                pullContext = messageExchangeService.extractProcessOnMpc(mpc);
            } else {
                throw pme;
            }
        }
        LOG.debug("Retrieve ready to pull User message for mpc: [{}]", pullRequest.getMpc());
        String messageId = messageExchangeService.retrieveReadyToPullUserMessageId(pullRequest.getMpc(), pullContext.getInitiator());

        String refToMessageId = (messaging.getSignalMessage().getMessageInfo() == null) ? null :
                messaging.getSignalMessage().getMessageInfo().getMessageId();
        return pullRequestHandler.handlePullRequest(messageId, pullContext, refToMessageId);
    }
}
