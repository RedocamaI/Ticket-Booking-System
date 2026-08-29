package com.redocmi.api_gateway.config;

import com.redocmi.api_gateway.filter.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class GatewayConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(
            JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);

        registration.addUrlPatterns("/*");
        registration.setOrder(1);

        return registration;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
