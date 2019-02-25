package eu.domibus.messaging;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.pull.PullReceiptListener;
import eu.domibus.ebms3.sender.MessageSender;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@Configuration
public class MessageListenerContainerConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageListenerContainerConfiguration.class);
    private static final String DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY = "domibus.pull.receipt.queue.concurrency";

    @Autowired
    @Qualifier("sendMessageQueue")
    private Queue sendMessageQueue;

    @Autowired
    @Qualifier("sendPullReceiptQueue")
    private Queue sendPullReceiptQueue;

    @Autowired
    @Qualifier("messageSenderService")
    private MessageSender messageSenderService;

    @Autowired
    @Qualifier("pullReceiptListener")
    private PullReceiptListener pullReceiptListener;

    @Autowired
    @Qualifier("domibusJMS-XAConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Bean(name = "dispatchContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer messageListenerContainer(Domain domain) {
        LOG.debug("Instantiating the DefaultMessageListenerContainer for domain [{}]", domain);
        return create(domain);
    }

    protected DefaultMessageListenerContainer create(Domain domain) {
        LOG.trace("create DefaultMessageListenerContainer for [{}]", domain);

        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();

        messageListenerContainer.setMessageSelector(MessageConstants.DOMAIN + "='" + domain.getCode() + "'");

        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(sendMessageQueue);
        messageListenerContainer.setMessageListener(messageSenderService);
        messageListenerContainer.setTransactionManager(transactionManager);
        messageListenerContainer.setConcurrency(domibusPropertyProvider.getDomainProperty(domain, "domibus.dispatcher.concurency"));
        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(0);

        messageListenerContainer.afterPropertiesSet();

        return messageListenerContainer;
    }

    @Bean(name = "pullReceiptContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createPullReceiptListener(Domain domain) {
        LOG.debug("Instantiating the createPullReceiptListener for domain [{}]", domain);
        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();

        messageListenerContainer.setMessageSelector(MessageConstants.DOMAIN + "='" + domain.getCode() + "'");

        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(sendPullReceiptQueue);
        messageListenerContainer.setMessageListener(pullReceiptListener);
        messageListenerContainer.setTransactionManager(transactionManager);
        messageListenerContainer.setConcurrency(domibusPropertyProvider.getDomainProperty(domain, DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY));
        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(0);

        messageListenerContainer.afterPropertiesSet();

        return messageListenerContainer;
    }

}
