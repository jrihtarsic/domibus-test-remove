package eu.domibus.core.plugin.delegate;

import eu.domibus.common.MessageReceiveFailureEvent;
import eu.domibus.common.MessageStatusChangeEvent;
import eu.domibus.plugin.BackendConnector;

/**
 * @author Cosmin Baciu
 * @since 3.2.2
 */
public interface BackendConnectorDelegate {

    void messageReceiveFailed(BackendConnector backendConnector, MessageReceiveFailureEvent event);

    void messageStatusChanged(BackendConnector backendConnector, MessageStatusChangeEvent event);
}
