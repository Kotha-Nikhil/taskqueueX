package com.taskqueuex.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    @Value("${worker.id}")
    private String workerId;

    @Bean
    public String workerId() {
        return workerId;
    }
}
