package com.ecodana.evodanavn1.config;

import com.ecodana.evodanavn1.client.PayOSClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOSConfig {

    @Bean
    public PayOSClient payOSClient() {
        return new PayOSClient();
    }
}
