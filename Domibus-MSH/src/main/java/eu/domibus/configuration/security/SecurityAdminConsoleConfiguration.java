package eu.domibus.configuration.security;

import eu.domibus.api.security.AuthRole;
import eu.domibus.common.services.impl.UserDetailServiceImpl;
import eu.domibus.security.AuthenticationService;
import eu.domibus.security.AuthenticationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.ws.rs.HttpMethod;

/**
 * Default Spring security config for Domibus
 *
 * @author Catalin Enache
 * @since 4.1
 */
@Conditional(SecurityInternalAuthProviderCondition.class)
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityAdminConsoleConfiguration extends AbstractWebSecurityConfigurerAdapter {

    @Autowired
    UserDetailServiceImpl userDetailService;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Bean(name = "authenticationManagerForAdminConsole")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean(name = "authenticationService")
    public AuthenticationService authenticationService() {
        return new AuthenticationServiceImpl();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailService);
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return provider;
    }

    @Override
    public void configureHttpSecurity(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeRequests()
                .antMatchers("/rest/testconnection/cn").permitAll()
                .antMatchers( "/rest/security/user/domain").hasAnyAuthority(AuthRole.ROLE_USER.name(), AuthRole.ROLE_ADMIN.name(), AuthRole.ROLE_AP_ADMIN.name());
    }

    @Override
    protected void configureWebSecurity(WebSecurity web) throws Exception {
        //nothing here
    }

    @Autowired
    @Override
    protected void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(daoAuthenticationProvider());
    }

}
