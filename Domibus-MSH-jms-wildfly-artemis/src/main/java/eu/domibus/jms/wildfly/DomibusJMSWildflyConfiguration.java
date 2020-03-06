package eu.domibus.jms.wildfly;

import eu.domibus.api.property.DomibusPropertyMetadataManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import java.lang.management.ManagementFactory;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration
public class DomibusJMSWildflyConfiguration {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(DomibusJMSWildflyConfiguration.class);

    public static final String MQ_BROKER_NAME = "org.apache.activemq.artemis:broker=&quot;%s&quot;";

    @Bean("mbeanServer")
    public MBeanServer mBeanServerConnectionFactoryBean() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Bean("activeMQServerControl")
    public MBeanProxyFactoryBean mBeanProxyFactoryBean(@Qualifier("mbeanServer") MBeanServer mBeanServer,
                                                       DomibusPropertyProvider domibusPropertyProvider) throws MalformedObjectNameException {
        MBeanProxyFactoryBean result = new MBeanProxyFactoryBean();

        String artemisBroker = domibusPropertyProvider.getProperty(DomibusPropertyMetadataManager.ACTIVE_MQ_ARTEMIS_BROKER);
        String objectName = String.format(MQ_BROKER_NAME, artemisBroker);
        LOGGER.debug("Configured property [objectName] with [{}]", objectName);

        result.setObjectName(objectName);
        result.setProxyInterface(ActiveMQServerControl.class);
        result.setServer(mBeanServer);

        return result;
    }

    @Bean("jmsSender")
    public JmsTemplate jmsSender(@Qualifier("domibusJMS-XAConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate result = new JmsTemplate();
        result.setSessionTransacted(true);
        result.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        result.setConnectionFactory(connectionFactory);

        return result;
    }
}
