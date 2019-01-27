package eu.domibus.weblogic.security;

import eu.domibus.configuration.security.AbstractWebSecurityConfigurerAdapter;
import eu.domibus.configuration.security.SecurityExternalAuthProviderCondition;
import eu.domibus.security.AuthenticationService;
import eu.domibus.web.filter.SetDomainFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring security configuration file for ECAS
 *
 * @author Catalin Enache
 * @since 4.1
 */
@Conditional(SecurityExternalAuthProviderCondition.class)
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class ECASSecurityConfiguration extends AbstractWebSecurityConfigurerAdapter {

    @Autowired
    CsrfTokenRepository tokenRepository;

    @Autowired
    RequestMatcher csrfURLMatcher;

    @Autowired
    SetDomainFilter setDomainFilter;

    @Autowired
    Http403ForbiddenEntryPoint http403ForbiddenEntryPoint;

    @Autowired
    ECASUserDetailsService ecasUserDetailsService;


    @Bean(name = "authenticationService")
    public AuthenticationService authenticationService() {
        return new ECASAuthenticationServiceImpl();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web
                .ignoring().antMatchers("/services/**")
                .and()
                .ignoring().antMatchers("/ext/**")
                .and()
                .ignoring().antMatchers("/logout/**")
                .and()
                .debug(true);
    }

    @Override
    public void configureHttpSecurity(HttpSecurity http) throws Exception {
        http
                .csrf().csrfTokenRepository(tokenRepository).requireCsrfProtectionMatcher(csrfURLMatcher)
                .and()
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/rest/security/authentication").permitAll()
                .antMatchers("/rest/application/info").permitAll()
                .antMatchers("/rest/application/name").permitAll()
                .antMatchers("/rest/application/fourcornerenabled").permitAll()
                .antMatchers("/rest/application/extauthproviderenabled").permitAll()
                .antMatchers("/rest/application/multitenancy").permitAll()
                .antMatchers("/rest/application/domains").hasRole(SUPER_ROLE)
                .antMatchers(HttpMethod.PUT, "/rest/security/user/password").authenticated()
                .antMatchers(HttpMethod.PUT, "/rest/security/user/domain").hasAnyRole(SUPER_ROLE)
                .antMatchers("/rest/pmode/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/party/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/truststore/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/messagefilters/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/jms/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/user/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/plugin/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/audit/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/alerts/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/testservice/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/logging/**").hasAnyRole(ADMIN_ROLES)
                .antMatchers("/rest/**").authenticated()
                .and()
                .jee().authenticatedUserDetailsService(ecasUserDetailsService)
                .and()
                .sessionManagement().sessionFixation().none()
                .and()
                .exceptionHandling().and()
                .headers().frameOptions().deny().contentTypeOptions().and().xssProtection().xssProtectionEnabled(true).and()
                .and()
                .httpBasic().authenticationEntryPoint(http403ForbiddenEntryPoint)
                .and()
                .addFilterBefore(setDomainFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Autowired
    @Override
    protected void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(ecasUserDetailsService);
    }



}
