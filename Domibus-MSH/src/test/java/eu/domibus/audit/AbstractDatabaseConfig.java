package eu.domibus.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Configuration
public abstract class AbstractDatabaseConfig {

    abstract DataSource dataSource();

    abstract Map<Object,String> getProperties();

    @Bean
    public LocalContainerEntityManagerFactoryBean domibusJTA() {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setPackagesToScan("eu.domibus");
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto","update");
        jpaProperties.put("hibernate.show_sql","true");
        jpaProperties.put("hibernate.format_sql","true");


        jpaProperties.putAll(getProperties());
        localContainerEntityManagerFactoryBean.setJpaProperties(jpaProperties);
        localContainerEntityManagerFactoryBean.setDataSource(dataSource());
        return localContainerEntityManagerFactoryBean;
    }


    @Bean
    public JpaTransactionManager jpaTransactionManager(){
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(domibusJTA().getObject());
        return jpaTransactionManager;
    }

}
