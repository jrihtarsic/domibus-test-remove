package eu.domibus.spring;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration("domibusConfiguration")
@ImportResource({
        "classpath:META-INF/cxf/cxf.xml",
        "classpath:META-INF/cxf/cxf-extension-jaxws.xml",
        "classpath:META-INF/cxf/cxf-servlet.xml",
        "classpath*:META-INF/resources/WEB-INF/spring-context.xml"
})
@ComponentScan(
        basePackages = "eu.domibus",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.web\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.ext\\.rest\\..*")}
)
@EnableJms
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class DomibusRootConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusRootConfiguration.class);


}
