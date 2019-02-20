package eu.domibus.tomcat.activemq;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Configuration
public class EmbeddedActiveMQConfiguration implements Condition {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(EmbeddedActiveMQConfiguration.class);

    @Value("${activeMQ.embedded.configurationFile}")
    Resource activeMQConfiguration;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final boolean embeddedActiveMQBroker = new EmbeddedActiveMQBrokerCondition().matches(context, metadata);
        LOGGER.debug("Condition result is [{}]", embeddedActiveMQBroker);
        return embeddedActiveMQBroker;
    }

    @Bean(name = "broker")
    @Conditional(EmbeddedActiveMQConfiguration.class)
    public BrokerFactoryBean activeMQBroker() {
        LOGGER.debug("Creating the embedded Active MQ broker from [{}]", activeMQConfiguration);
        final DomibusBrokerFactoryBean brokerFactoryBean = new DomibusBrokerFactoryBean();
        brokerFactoryBean.setConfig(activeMQConfiguration);
        return brokerFactoryBean;

    }
}
