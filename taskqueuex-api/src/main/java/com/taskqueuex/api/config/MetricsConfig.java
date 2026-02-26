package com.taskqueuex.api.config;

import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.repository.JobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.taskqueuex.common.queue.QueueConstants;

@Configuration
public class MetricsConfig {

    @Bean
    public Gauge queueDepthGauge(StringRedisTemplate redisTemplate, MeterRegistry registry) {
        return Gauge.builder("queue.depth", redisTemplate, template -> {
            try {
                Long size = template.opsForList().size(QueueConstants.JOB_QUEUE_NAME);
                return size != null ? size : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Current depth of the job queue")
        .register(registry);
    }

    @Bean
    public Gauge jobsQueuedGauge(JobRepository jobRepository, MeterRegistry registry) {
        return Gauge.builder("jobs.queued.count", jobRepository, repo ->
            repo.countByStatus(JobStatus.QUEUED))
        .description("Number of jobs in QUEUED status")
        .register(registry);
    }

    @Bean
    public Gauge jobsInProgressGauge(JobRepository jobRepository, MeterRegistry registry) {
        return Gauge.builder("jobs.in_progress.count", jobRepository, repo ->
            repo.countByStatus(JobStatus.IN_PROGRESS))
        .description("Number of jobs in IN_PROGRESS status")
        .register(registry);
    }
}
