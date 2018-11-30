package eu.domibus.messaging;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.ebms3.sender.LargeMessageSenderListener;
import eu.domibus.ebms3.sender.MessageSenderListener;
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

    @Autowired
    @Qualifier("sendMessageQueue")
    private Queue sendMessageQueue;

    @Autowired
    @Qualifier("sendLargeMessageQueue")
    private Queue sendLargeMessageQueue;

    @Autowired
    @Qualifier("messageSenderListener")
    private MessageSenderListener messageSenderListener;

    @Autowired
    @Qualifier("largeMessageSenderListener")
    private LargeMessageSenderListener largeMessageSenderListener;

    @Autowired
    @Qualifier("domibusJMS-XAConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Bean(name = "dispatchContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendMessageListener(Domain domain) {
        LOG.debug("Instantiating the DefaultMessageListenerContainer for domain [{}]", domain);
        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();

        messageListenerContainer.setMessageSelector(MessageConstants.DOMAIN + "='" + domain.getCode() + "'");

        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(sendMessageQueue);
        messageListenerContainer.setMessageListener(messageSenderListener);
        messageListenerContainer.setTransactionManager(transactionManager);
        messageListenerContainer.setConcurrency(domibusPropertyProvider.getDomainProperty(domain,"domibus.dispatcher.concurency"));
        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(0);

        messageListenerContainer.afterPropertiesSet();

        return messageListenerContainer;
    }

    @Bean(name = "sendLargeMessageContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendLargeMessageListener(Domain domain) {
        LOG.debug("Instantiating the createSendLargeMessageListenerContainer for domain [{}]", domain);
        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();

        messageListenerContainer.setMessageSelector(MessageConstants.DOMAIN + "='" + domain.getCode() + "'");

        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(sendLargeMessageQueue);
        messageListenerContainer.setMessageListener(largeMessageSenderListener);
        messageListenerContainer.setTransactionManager(transactionManager);
        messageListenerContainer.setConcurrency(domibusPropertyProvider.getDomainProperty(domain,"domibus.dispatcher.largeFiles.concurrency"));
        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(0);

        messageListenerContainer.afterPropertiesSet();

        return messageListenerContainer;
    }

}
