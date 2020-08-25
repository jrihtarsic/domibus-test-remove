package eu.domibus.web.spring;

import eu.domibus.ext.rest.spring.DomibusExtWebConfiguration;
import eu.domibus.web.converter.CustomMappingJackson2HttpMessageConverter;
import eu.domibus.web.rest.validators.RestQueryParamsValidationInterceptor;
import eu.domibus.web.security.AuthenticatedPrincipalInterceptor;
import eu.domibus.web.security.DefaultPasswordInterceptor;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author Cosmin Baciu, Ion Perpegel
 * @since 4.2
 * <p>
 * Java configuration (that replaces mvc-dispatcher-servlet) for configuring dispatcher servlet
 */
@EnableWebMvc
@Configuration("domibusWebConfiguration")
@Import(DomibusExtWebConfiguration.class)
@ComponentScan(basePackages = "eu.domibus.web")
@ImportResource("classpath*:config/*-domibusServlet.xml")
public class DomibusWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(defaultPasswordInterceptor())
                .addPathPatterns("/rest/**")
                .excludePathPatterns("/rest/security/user")
                .excludePathPatterns("/rest/security/username")
                .excludePathPatterns("/rest/security/authentication")
                .excludePathPatterns("/rest/security/user/domain")
                .excludePathPatterns("/rest/security/user/password")
                .excludePathPatterns("/rest/application/**");

        registry.addInterceptor(restQueryParamsValidationInterceptor())
                .addPathPatterns("/rest/**");

        registry.addInterceptor(authenticatedPrincipalInterceptor())
                .addPathPatterns("/rest/**");

    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, customMappingJackson2HttpMessageConverter());
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
    }

    @Bean
    DefaultPasswordInterceptor defaultPasswordInterceptor() {
        return new DefaultPasswordInterceptor();
    }

    @Bean
    RestQueryParamsValidationInterceptor restQueryParamsValidationInterceptor() {
        return new RestQueryParamsValidationInterceptor();
    }

    @Bean
    AuthenticatedPrincipalInterceptor authenticatedPrincipalInterceptor() {
        return new AuthenticatedPrincipalInterceptor();
    }

    @Bean
    CustomMappingJackson2HttpMessageConverter customMappingJackson2HttpMessageConverter() {
        CustomMappingJackson2HttpMessageConverter bean = new CustomMappingJackson2HttpMessageConverter();
        bean.setJsonPrefix(")]}',\n");
        return bean;
    }

    @Bean
    public javax.validation.Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
