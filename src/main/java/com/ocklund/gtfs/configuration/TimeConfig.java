package com.ocklund.gtfs.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    
    @Bean
    public TimeProvider timeProvider() {
        return new DefaultTimeProvider();
    }
}
