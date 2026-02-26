package com.taskqueuex.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.taskqueuex")
@EntityScan(basePackages = "com.taskqueuex.common.entity")
@EnableJpaRepositories(basePackages = "com.taskqueuex.common.repository")
@EnableScheduling
public class TaskQueueXWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskQueueXWorkerApplication.class, args);
    }
}
