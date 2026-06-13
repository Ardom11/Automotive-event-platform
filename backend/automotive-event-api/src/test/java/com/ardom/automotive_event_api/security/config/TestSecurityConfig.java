package com.ardom.automotive_event_api.security.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true)
@EnableWebSecurity
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated()
                )
                .build();
    }

    public static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/events/**",
            "/error"
    };

}
