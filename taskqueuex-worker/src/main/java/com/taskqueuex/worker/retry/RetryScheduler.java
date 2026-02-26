package com.taskqueuex.worker.retry;

import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.queue.QueueConstants;
import com.taskqueuex.common.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RetryScheduler.class);

    private final JobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public RetryScheduler(
            JobRepository jobRepository,
            StringRedisTemplate redisTemplate,
            @Value("${worker.retry-scheduler.enabled:true}") boolean enabled) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${worker.retry-scheduler.interval-seconds:10}000")
    @Transactional
    public void scheduleRetries() {
        if (!enabled) {
            return;
        }

        try {
            List<Job> jobsReadyForRetry = jobRepository.findJobsReadyForRetry(
                JobStatus.RETRY_SCHEDULED,
                Instant.now()
            );

            if (jobsReadyForRetry.isEmpty()) {
                return;
            }

            logger.info("Found {} jobs ready for retry", jobsReadyForRetry.size());

            for (Job job : jobsReadyForRetry) {
                // Double-check status and next_retry_at to avoid race conditions
                if (job.getStatus() == JobStatus.RETRY_SCHEDULED && 
                    job.getNextRetryAt() != null && 
                    job.getNextRetryAt().isBefore(Instant.now())) {
                    
                    // Change status back to QUEUED and re-enqueue
                    job.setStatus(JobStatus.QUEUED);
                    job.setNextRetryAt(null);
                    jobRepository.save(job);

                    redisTemplate.opsForList().rightPush(QueueConstants.JOB_QUEUE_NAME, job.getId().toString());
                    logger.info("Re-enqueued job {} for retry (attempt {}/{})", 
                        job.getId(), job.getRetryCount() + 1, job.getMaxRetries());
                }
            }
        } catch (Exception e) {
            logger.error("Error in retry scheduler", e);
        }
    }
}
