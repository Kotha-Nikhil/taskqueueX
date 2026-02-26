package com.taskqueuex.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.taskqueuex")
@EntityScan(basePackages = "com.taskqueuex.common.entity")
@EnableJpaRepositories(basePackages = "com.taskqueuex.common.repository")
public class TaskQueueXApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskQueueXApiApplication.class, args);
    }
}
