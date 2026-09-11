package com.redocmi.api_gateway;

import com.redocmi.api_gateway.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class ApiGatewayApplication {

	static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
