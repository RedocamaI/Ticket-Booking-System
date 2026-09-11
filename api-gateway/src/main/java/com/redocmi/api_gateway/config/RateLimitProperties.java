package com.redocmi.api_gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitProperties {
    private int auth;
    private int defaultLimit;
    private long windowMs;
}
