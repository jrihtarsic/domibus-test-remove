package eu.domibus.core.jms.multitenancy;

import eu.domibus.api.jms.JMSConstants;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.ebms3.sender.MessageSenderListener;
import eu.domibus.core.message.pull.PullMessageSender;
import eu.domibus.core.message.pull.PullReceiptListener;
import eu.domibus.core.message.retention.*;
import eu.domibus.core.message.splitandjoin.LargeMessageSenderListener;
import eu.domibus.core.message.splitandjoin.SplitAndJoinListener;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.util.Optional;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@Configuration
public class MessageListenerContainerConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageListenerContainerConfiguration.class);
    public static final String PROPERTY_LARGE_FILES_CONCURRENCY = DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY;
    public static final String PROPERTY_SPLIT_AND_JOIN_CONCURRENCY = DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY;
    private static final String PROPERTY_RETENTION_JMS_CONCURRENCY = DOMIBUS_RETENTION_JMS_CONCURRENCY;


    @Autowired
    @Qualifier("sendMessageQueue")
    private Queue sendMessageQueue;

    @Autowired
    @Qualifier("dlqQueue")
    private Queue dlqQueue;

    @Autowired
    @Qualifier("sendPullReceiptQueue")
    private Queue sendPullReceiptQueue;

    @Autowired
    @Qualifier("pullMessageQueue")
    private Queue pullMessageQueue;

    @Autowired
    @Qualifier("sendLargeMessageQueue")
    private Queue sendLargeMessageQueue;

    @Autowired
    @Qualifier("splitAndJoinQueue")
    private Queue splitAndJoinQueue;

    @Autowired
    @Qualifier("retentionMessageQueue")
    private Queue retentionMessageQueue;

    @Autowired
    @Qualifier("messageSenderListener")
    private MessageSenderListener messageSenderListener;

    @Autowired
    @Qualifier("largeMessageSenderListener")
    private LargeMessageSenderListener largeMessageSenderListener;

    @Autowired
    @Qualifier("splitAndJoinListener")
    private SplitAndJoinListener splitAndJoinListener;

    @Autowired
    @Qualifier("pullReceiptListener")
    private PullReceiptListener pullReceiptListener;

    @Autowired
    @Qualifier("retentionListener")
    private RetentionListener retentionListener;

    @Autowired
    protected LowDLQListener lowDLQListener;

    @Autowired
    protected MediumDLQListener mediumDLQListener;

    @Autowired
    protected HighDLQListener highDLQListener;

    @Autowired
    protected DefaultDLQListener defaultDLQListener;

    @Autowired
    PullMessageSender pullMessageListener;

    @Autowired
    @Qualifier(JMSConstants.DOMIBUS_JMS_XACONNECTION_FACTORY)
    private ConnectionFactory connectionFactory;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    Optional<JndiDestinationResolver> internalDestinationResolver;

    @Bean(name = "dlqListenerLowPriority")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createDlqListenerLowPriority(Domain domain, String selector, String concurrency) {
        LOG.debug("Instantiating the dlq listener for domain [{}] with selector [{}] and concurrency [{}]", domain, selector, concurrency);
        return createDefaultMessageListenerContainer(domain, connectionFactory, dlqQueue,
                lowDLQListener, transactionManager, concurrency, selector
        );
    }

    @Bean(name = "dlqListenerMediumPriority")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createDlqListenerMediumPriority(Domain domain, String selector, String concurrency) {
        LOG.debug("Instantiating the dlq listener for domain [{}] with selector [{}] and concurrency [{}]", domain, selector, concurrency);
        return createDefaultMessageListenerContainer(domain, connectionFactory, dlqQueue,
                mediumDLQListener, transactionManager, concurrency, selector
        );
    }

    @Bean(name = "dlqListenerHighPriority")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createDlqListenerHighPriority(Domain domain, String selector, String concurrency) {
        LOG.debug("Instantiating the dlq listener for domain [{}] with selector [{}] and concurrency [{}]", domain, selector, concurrency);
        return createDefaultMessageListenerContainer(domain, connectionFactory, dlqQueue,
                highDLQListener, transactionManager, concurrency, selector
        );
    }

    @Bean(name = "dlqListenerDefaultPriority")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createDlqListenerDefaultPriority(Domain domain, String selector, String concurrency) {
        LOG.debug("Instantiating the dlq listener for domain [{}] with selector [{}] and concurrency [{}]", domain, selector, concurrency);
        return createDefaultMessageListenerContainer(domain, connectionFactory, dlqQueue,
                defaultDLQListener, transactionManager, concurrency, selector
        );
    }

    @Bean(name = "dispatchContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendMessageListener(Domain domain) {
        LOG.debug("Instantiating the DefaultMessageListenerContainer for domain [{}]", domain);
        return createDefaultMessageListenerContainer(domain, connectionFactory, sendMessageQueue,
                messageSenderListener, transactionManager, DOMIBUS_DISPATCHER_CONCURENCY
        );
    }

    /**
     * Creates the large message JMS listener(domain dependent)
     *
     * @param domain
     * @return
     */
    @Bean(name = "sendLargeMessageContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendLargeMessageListener(Domain domain) {
        LOG.debug("Instantiating the createSendLargeMessageListenerContainer for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, sendLargeMessageQueue,
                largeMessageSenderListener, transactionManager, PROPERTY_LARGE_FILES_CONCURRENCY
        );
    }

    /**
     * Creates the SplitAndJoin JMS listener(domain dependent)
     *
     * @param domain
     * @return
     */
    @Bean(name = "splitAndJoinContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSplitAndJoinListener(Domain domain) {
        LOG.debug("Instantiating the createSplitAndJoinListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, splitAndJoinQueue,
                splitAndJoinListener, transactionManager, PROPERTY_SPLIT_AND_JOIN_CONCURRENCY
        );
    }

    @Bean(name = "pullReceiptContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createPullReceiptListener(Domain domain) {
        LOG.debug("Instantiating the createPullReceiptListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, sendPullReceiptQueue,
                pullReceiptListener, transactionManager, DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY
        );
    }

    /**
     * Creates the Retention Message JMS listener (domain dependent)
     *
     * @param domain the domain to which this bean is created for
     * @return the retention listener prototype bean dedicated to the provided domain
     */
    @Bean(name = "retentionContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createRetentionListener(Domain domain) {
        LOG.debug("Instantiating the createRetentionListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, retentionMessageQueue,
                retentionListener, transactionManager, PROPERTY_RETENTION_JMS_CONCURRENCY);
    }

    @Bean(name = "pullMessageContainer")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createPullMessageListener(Domain domain) {
        LOG.debug("Instantiating the pullMessageListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, pullMessageQueue,
                pullMessageListener::processPullRequest, transactionManager, DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY, true);
    }


    /**
     * It will create a {@code DefaultMessageListenerContainer}
     *
     * @param domain                    domain
     * @param connectionFactory         JMS connection factory
     * @param destination               JMS queue
     * @param messageListener           JMS message listener
     * @param transactionManager        Transaction manager
     * @param domainPropertyConcurrency domain property key for retrieving queue concurrency value
     * @return
     */

    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener, PlatformTransactionManager transactionManager,
                                                                                  String domainPropertyConcurrency) {
        return createDefaultMessageListenerContainer(domain, connectionFactory, destination, messageListener, transactionManager, domainPropertyConcurrency, false, null);
    }

    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener, PlatformTransactionManager transactionManager,
                                                                                  String domainPropertyConcurrency, String selector) {
        return createDefaultMessageListenerContainer(domain, connectionFactory, destination, messageListener, transactionManager, domainPropertyConcurrency, false, selector);
    }

    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener, PlatformTransactionManager transactionManager,
                                                                                  String domainPropertyConcurrency, boolean useInternalDestinationResolver) {
        return createDefaultMessageListenerContainer(domain, connectionFactory, destination, messageListener, transactionManager, domainPropertyConcurrency, false, null);
    }


    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener, PlatformTransactionManager transactionManager,
                                                                                  String domainPropertyConcurrency, boolean useInternalDestinationResolver, String selector) {
        DefaultMessageListenerContainer messageListenerContainer = new DomainMessageListenerContainer(domain);

        String messageSelector = MessageConstants.DOMAIN + "='" + domain.getCode() + "'";
        if (StringUtils.isNotBlank(selector)) {
            messageSelector += " AND " + selector;
        }

        LOG.error("-------------Using message selector [{}]", messageSelector);

        if (StringUtils.isNotBlank(messageSelector)) {
            messageListenerContainer.setMessageSelector(messageSelector);
        }


        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(destination);
        messageListenerContainer.setMessageListener(messageListener);
        messageListenerContainer.setTransactionManager(transactionManager);

        final String concurrency = domibusPropertyProvider.getProperty(domain, domainPropertyConcurrency);
        messageListenerContainer.setConcurrency(concurrency);
        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(0);

        messageListenerContainer.afterPropertiesSet();

        if (useInternalDestinationResolver && internalDestinationResolver.isPresent()) {
            messageListenerContainer.setDestinationResolver(internalDestinationResolver.get());
        }

        LOG.debug("DefaultMessageListenerContainer initialized for domain [{}] with concurrency=[{}]", domain, concurrency);
        return messageListenerContainer;
    }

}
