package eu.domibus.sti;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServlet;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import eu.domibus.plugin.webService.generated.BackendInterface;
import eu.domibus.plugin.webService.generated.BackendService11;
import eu.domibus.rest.client.ApiClient;
import eu.domibus.rest.client.api.UsermessageApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.jms.QueueConnectionFactory;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@org.springframework.context.annotation.Configuration
@EnableJms
@EnableAsync
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private static final String CONNECTION_FACTORY_JNDI = "jms/ConnectionFactory";

    // Url to access to the queue o topic
    @Value("${jms.providerUrl}")
    private String providerUrl;

    // Jms connection type.
    @Value("${jms.connectionType}")
    private String connectionType;

    // Name of the queue o topic to extract the message
    @Value("${jms.destinationName}")
    private String destinationName;

    @Value("${jms.in.queue}")
    private String jmsInDestination;

    // Number of consumers in the application
    @Value("${jms.concurrentConsumers}")
    private String concurrentConsumers;

    @Value("${ws.plugin.url}")
    private String wsdlUrl;


    @Value("${domibus.url}")
    private String domibusUrl;

    @Value("${session.cache.size}")
    private Integer cacheSize;

    @Value("${jms.session.transacted}")
    private Boolean sessionTransacted;

    @Bean
    public DefaultJmsListenerContainerFactory myFactory() {
        LOG.info("Initiating jms listener factory");
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setDestinationResolver(jmsDestinationResolver());
        factory.setConcurrency(concurrentConsumers);

        if(sessionTransacted) {
            LOG.info("Configuring DefaultJmsListenerContainerFactory: myFactory with session transacted");
            factory.setSessionTransacted(true);
            factory.setSessionAcknowledgeMode(0);
        }

        return factory;
    }

    @Bean
    public JndiTemplate provider() {
        LOG.info("Configuring provider to:[{}]", providerUrl);
        Properties env = new Properties();
        env.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
        env.put("java.naming.provider.url", providerUrl);
        return new JndiTemplate(env);
    }

    @Bean
    public QueueConnectionFactory connectionFactory() {
        try {
            QueueConnectionFactory lookup = (QueueConnectionFactory) provider().lookup(CONNECTION_FACTORY_JNDI);
            CachingConnectionFactory factory = new CachingConnectionFactory(lookup);
            factory.setSessionCacheSize(cacheSize);
            return factory;
        } catch (NamingException e) {
            LOG.error("Impossible to get connection factory:[{}]", CONNECTION_FACTORY_JNDI, e);
            throw new IllegalStateException("Impossible to get connection factory");
        }
    }

    @Bean
    public JndiDestinationResolver jmsDestinationResolver() {
        JndiDestinationResolver destResolver = new JndiDestinationResolver();
        destResolver.setJndiTemplate(provider());
        return destResolver;
    }

    @Bean
    public JndiObjectFactoryBean inQueueDestination() {
        JndiObjectFactoryBean dest = new JndiObjectFactoryBean();
        dest.setJndiTemplate(provider());
        dest.setJndiName(jmsInDestination);
        return dest;
    }

    @Bean("stiUUIDGenerator")
    public NoArgGenerator createUUIDGenerator() {
        final EthernetAddress ethernetAddress = EthernetAddress.fromInterface();
        return Generators.timeBasedGenerator(ethernetAddress);
    }

    @Bean
    public JmsListener jmsListener() {
        return new JmsListener(senderService(), metricRegistry());
    }

    @Bean
    public JmsTemplate inQueueJmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
        LOG.info("Configuring jms template for");
        jmsTemplate.setDefaultDestinationName(jmsInDestination);
        jmsTemplate.setDestinationResolver(jmsDestinationResolver());
        jmsTemplate.setReceiveTimeout(5000);
        return jmsTemplate;
    }

    @Bean
    public BackendInterface backendInterface() {
        BackendService11 backendService = null;
        try {
            backendService = new BackendService11(new URL(wsdlUrl), new QName("http://org.ecodex.backend/1_1/", "BackendService_1_1"));
        } catch (MalformedURLException e) {
            LOG.error("Could not instantiate backendService, sending message with ws plugin won't work.", e);
            return null;
        }
        BackendInterface backendPort = backendService.getBACKENDPORT();

        //enable chunking
        BindingProvider bindingProvider = (BindingProvider) backendPort;
        //comment the following lines if sending large files
        List<Handler> handlers = bindingProvider.getBinding().getHandlerChain();
        bindingProvider.getBinding().setHandlerChain(handlers);

        return backendPort;

    }

  /*  @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        LOG.info("ThreadPool executor used for sending messages configured with size:[{}]",sendingThreadPoolSize);
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setMaxPoolSize(sendingThreadPoolSize);
        threadPoolTaskExecutor.setCorePoolSize(sendingThreadPoolSize);
        //threadPoolTaskExecutor.setQueueCapacity(sendingThreadPoolSize);
        return threadPoolTaskExecutor;
    }*/

    @Bean
    public UsermessageApi usermessageApi() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(domibusUrl);

        return new UsermessageApi(apiClient);
    }

    @Bean
    public SenderService senderService() {
        return new SenderService(inQueueJmsTemplate(), backendInterface(), metricRegistry(), usermessageApi(), createUUIDGenerator());
    }

    @Bean
    public HealthCheckRegistry healthCheckRegistry() {
        return new HealthCheckRegistry();
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public ConsoleReporter consoleReporter() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(30, TimeUnit.SECONDS);
        return reporter;
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        return new ServletRegistrationBean(new AdminServlet(), "/metrics/*");
    }
}