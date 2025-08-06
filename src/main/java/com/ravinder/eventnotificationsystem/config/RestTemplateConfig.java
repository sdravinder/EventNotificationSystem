package com.ravinder.eventnotificationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for HTTP client components.
 * 
 * This configuration provides:
 * - RestTemplate bean for HTTP operations
 * - Timeout configurations for callback requests
 * - Connection pool settings
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Configure RestTemplate with timeout settings for callback requests.
     * 
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds connection timeout
        factory.setReadTimeout(10000);   // 10 seconds read timeout
        
        return new RestTemplate(factory);
    }
}
