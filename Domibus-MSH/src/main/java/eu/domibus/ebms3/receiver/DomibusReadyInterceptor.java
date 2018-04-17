package eu.domibus.ebms3.receiver;

import eu.domibus.common.System;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Thomas Dussart
 * @since 3.3.4
 */

public class DomibusReadyInterceptor extends AbstractPhaseInterceptor {

    @Autowired
    private System system;

    public DomibusReadyInterceptor() {
        super("receive");
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (system.isNotReady()) {
            throw new Fault(new IllegalStateException("Server starting"));
        }
    }

}
