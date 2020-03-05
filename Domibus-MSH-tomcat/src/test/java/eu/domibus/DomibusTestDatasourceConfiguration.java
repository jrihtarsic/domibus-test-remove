package eu.domibus;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.tomcat.jpa.DomibusDatasourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration
public class DomibusTestDatasourceConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusTestDatasourceConfiguration.class);

    @Primary
    @Bean(name = "domibusJDBC-XADataSource")
    public DataSource xaDatasource() {
        return createDatasource();
    }

    @Primary
    @Bean("domibusJDBC-nonXADataSource")
    public DataSource nonXADatasource() {
        return createDatasource();
    }

    protected DriverManagerDataSource createDatasource() {
        DriverManagerDataSource result = new DriverManagerDataSource();
        result.setDriverClassName("org.h2.Driver");
        result.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        result.setUsername("sa");
        result.setPassword("");
        return result;
    }



}
