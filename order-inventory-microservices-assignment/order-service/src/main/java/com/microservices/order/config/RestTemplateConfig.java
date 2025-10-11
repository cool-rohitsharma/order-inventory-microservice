package com.microservices.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used for inter-service communication
 */
@Configuration
public class RestTemplateConfig {
    
    @Value("${inventory.service.timeout:30000}")
    private int timeout;
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}