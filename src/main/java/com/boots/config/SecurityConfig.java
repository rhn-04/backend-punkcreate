package com.boots.config;

import com.boots.service.UserProfileServiceImpl;
import com.boots.utils.JwtTokenUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserProfileServiceImpl userProfileService;
    private final JwtTokenUtils jwtTokenUtils;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UserProfileServiceImpl userProfileService, JwtTokenUtils jwtTokenUtils, JwtRequestFilter jwtRequestFilter) {
        this.userProfileService = userProfileService;
        this.jwtTokenUtils = jwtTokenUtils;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers("/register").permitAll()
                .antMatchers("/auth").permitAll()
                .antMatchers("/post/{id}").permitAll()
                .antMatchers("/").permitAll()
                .antMatchers("/search").permitAll()
                .antMatchers("/submit").authenticated()
                .antMatchers("/report").hasRole("MODERATOR")
                .antMatchers("/admin").hasRole("ADMIN")
                .and()
                .logout().logoutUrl("/logout").logoutSuccessUrl("/")
                .and()
                .formLogin().disable()
                .exceptionHandling().authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/auth");
                })
                .and()
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
