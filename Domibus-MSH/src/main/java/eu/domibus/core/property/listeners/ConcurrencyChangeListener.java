package eu.domibus.core.property.listeners;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.messaging.MessageListenerContainerInitializer;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Handles the change of concurrency properties
 */
@Service
public class ConcurrencyChangeListener implements PluginPropertyChangeListener {

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    MessageListenerContainerInitializer messageListenerContainerInitializer;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsAnyIgnoreCase(propertyName,
                DOMIBUS_DISPATCHER_CONCURENCY,
                DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY,
                DOMIBUS_RETENTION_JMS_CONCURRENCY,
                DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY,
                "domibus.pull.receipt.queue.concurrency",
                "domibus.pull.queue.concurency");
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        final Domain domain = domainService.getDomain(domainCode);

        switch (propertyName) {
            case DOMIBUS_DISPATCHER_CONCURENCY:
                messageListenerContainerInitializer.createSendMessageListenerContainer(domain);
                break;
            case DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY:
                messageListenerContainerInitializer.createSendLargeMessageListenerContainer(domain);
                break;
            case DOMIBUS_RETENTION_JMS_CONCURRENCY:
                messageListenerContainerInitializer.createRetentionListenerContainer(domain);
                break;
            case DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY:
                messageListenerContainerInitializer.createSplitAndJoinListenerContainer(domain);
                break;
            case "domibus.pull.receipt.queue.concurrency":
                messageListenerContainerInitializer.createPullReceiptListenerContainer(domain);
                break;
            case "domibus.pull.queue.concurency":
                messageListenerContainerInitializer.createPullMessageListenerContainer(domain);
                break;
        }
    }
}
