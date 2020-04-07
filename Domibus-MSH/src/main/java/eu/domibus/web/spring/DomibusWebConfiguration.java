package eu.domibus.web.spring;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.ext.rest.spring.DomibusExtWebConfiguration;
import eu.domibus.web.converter.CustomMappingJackson2HttpMessageConverter;
import eu.domibus.web.rest.validators.RestQueryParamsValidationInterceptor;
import eu.domibus.web.security.AuthenticatedPrincipalInterceptor;
import eu.domibus.web.security.DefaultPasswordInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_FILE_UPLOAD_MAX_SIZE;

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
public class DomibusWebConfiguration implements WebMvcConfigurer {

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

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
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(customMappingJackson2HttpMessageConverter());

        List<HttpMessageConverter<?>> defaultConverters = new WebMvcConfigurationSupport() {
            public List<HttpMessageConverter<?>> getDefaultMessageConverters() {
                return this.getMessageConverters();
            }
        }.getDefaultMessageConverters();

        converters.addAll(defaultConverters);
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
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        int size = domibusPropertyProvider.getIntegerProperty(DOMIBUS_FILE_UPLOAD_MAX_SIZE);
        resolver.setMaxUploadSize(size);
        return resolver;
    }

}
