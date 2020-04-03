package eu.domibus.web.core.spring;

import eu.domibus.web.converter.CustomMappingJackson2HttpMessageConverter;
import eu.domibus.web.rest.validators.RestQueryParamsValidationInterceptor;
import eu.domibus.web.security.AuthenticatedPrincipalInterceptor;
import eu.domibus.web.security.DefaultPasswordInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@EnableWebMvc
@Configuration("domibusWebConfiguration")
@ImportResource({"classpath*:config/*-domibusServlet.xml"})
@ComponentScan(basePackages = "eu.domibus.web")
public class DomibusWebConfiguration implements WebMvcConfigurer {

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
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
    }
}
