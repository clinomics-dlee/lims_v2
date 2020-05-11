package com.clinomics.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	UserDetailsService userDetailsService;

	public AuthenticationSuccessHandler successHandler() throws Exception{
		return new SecurityLoginSuccessHandler();
	}
	public SecurityLogoutSuccessHandler logoutSuccessHandler() throws Exception{
		return new SecurityLogoutSuccessHandler();
	}
	public AuthenticationFailureHandler failureHandler() throws Exception{
		return new SecurityLoginFailureHandler();
	}
	
	@Bean
    public SpringSecurityDialect springSecurityDialect(){
        return new SpringSecurityDialect();
    }

	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
	public void configure(WebSecurity web) throws Exception{
		web.ignoring().antMatchers("/assets/**");
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf()
				.ignoringAntMatchers("/logout")
			.and()
				.authorizeRequests()
					.antMatchers("/login/**", "/reg/**", "/registration", "/j_spring_security_check").permitAll()
			.anyRequest()
				.authenticated()
			.and()
				.formLogin().loginPage("/login").loginProcessingUrl("/j_spring_security_check").successHandler(successHandler())
			.and()
				.logout().logoutSuccessHandler(logoutSuccessHandler());
	}

	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
}
