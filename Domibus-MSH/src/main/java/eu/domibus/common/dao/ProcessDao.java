package eu.domibus.common.dao;

import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.context.MessageExchangeContext;

import java.util.List;

/**
 * Created by dussath on 5/18/17.
 * Data acces for process entity.
 */
public interface ProcessDao {
    /**
     * Search for processes that correspond to the message exchange configuration.
     * @param messageExchangeContext contains information about the exchange.
     * @return the corresponding processes.
     */
    List<Process> findProcessForMessageContext(MessageExchangeContext messageExchangeContext);
}
