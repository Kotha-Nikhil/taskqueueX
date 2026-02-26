package com.taskqueuex.worker.consumer;

import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.entity.JobAttempt;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.queue.QueueConstants;
import com.taskqueuex.common.repository.JobAttemptRepository;
import com.taskqueuex.common.repository.JobRepository;
import com.taskqueuex.worker.handler.JobHandler;
import com.taskqueuex.worker.handler.JobHandlerRegistry;
import com.taskqueuex.worker.service.JobClaimService;
import com.taskqueuex.worker.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class QueueConsumer {

    private static final Logger logger = LoggerFactory.getLogger(QueueConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final JobClaimService jobClaimService;
    private final JobHandlerRegistry handlerRegistry;
    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;
    private final StorageService storageService;
    private final String workerId;
    private final int pollTimeoutSeconds;

    public QueueConsumer(
            StringRedisTemplate redisTemplate,
            JobClaimService jobClaimService,
            JobHandlerRegistry handlerRegistry,
            JobRepository jobRepository,
            JobAttemptRepository jobAttemptRepository,
            StorageService storageService,
            @Value("${worker.id}") String workerId,
            @Value("${worker.queue.poll-timeout-seconds:5}") int pollTimeoutSeconds) {
        this.redisTemplate = redisTemplate;
        this.jobClaimService = jobClaimService;
        this.handlerRegistry = handlerRegistry;
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
        this.storageService = storageService;
        this.workerId = workerId;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${worker.queue.poll-interval-ms:1000}")
    public void pollAndProcess() {
        try {
            // Non-blocking pop from Redis queue (we rely on @Scheduled for polling)
            String jobIdStr = redisTemplate.opsForList().rightPop(QueueConstants.JOB_QUEUE_NAME);
            
            if (jobIdStr == null) {
                return; // No job available
            }

            UUID jobId = UUID.fromString(jobIdStr);
            logger.debug("Popped job {} from queue", jobId);

            // Claim the job
            Optional<Job> jobOpt = jobClaimService.claimJob(jobId, workerId);
            if (jobOpt.isEmpty()) {
                logger.debug("Could not claim job {}, re-enqueuing", jobId);
                // Re-enqueue if claim failed
                redisTemplate.opsForList().rightPush(QueueConstants.JOB_QUEUE_NAME, jobIdStr);
                return;
            }

            Job job = jobOpt.get();
            processJob(job.getId());

        } catch (Exception e) {
            logger.error("Error in queue consumer", e);
        }
    }

    @Transactional
    public void processJob(UUID jobId) {
        // Re-fetch the job within this transaction to get the latest version
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        JobAttempt attempt = new JobAttempt();
        attempt.setJob(job);
        attempt.setWorkerId(workerId);
        attempt.setAttemptNumber(job.getRetryCount() + 1);
        attempt.setStatus("STARTED");
        attempt.setStartedAt(Instant.now());
        attempt = jobAttemptRepository.save(attempt);

        try {
            // Get handler for job type
            JobHandler handler = handlerRegistry.getHandler(job.getJobType());
            
            // Execute handler
            handler.handle(job);

            // If handler succeeded, update job status
            job.setStatus(JobStatus.SUCCEEDED);
            job.setCompletedAt(Instant.now());
            
            attempt.setStatus("SUCCEEDED");
            attempt.setFinishedAt(Instant.now());
            jobAttemptRepository.save(attempt);
            jobRepository.save(job);

            logger.info("Successfully processed job {}", job.getId());

        } catch (Exception e) {
            logger.error("Error processing job {}", job.getId(), e);
            handleJobFailure(job, attempt, e);
        }
    }

    private void handleJobFailure(Job job, JobAttempt attempt, Exception e) {
        attempt.setStatus("FAILED");
        attempt.setFinishedAt(Instant.now());
        attempt.setErrorMessage(e.getMessage());
        jobAttemptRepository.save(attempt);

        int newRetryCount = job.getRetryCount() + 1;
        job.setRetryCount(newRetryCount);
        job.setLastError(e.getMessage());

        if (newRetryCount >= job.getMaxRetries()) {
            job.setStatus(JobStatus.DEAD_LETTER);
            job.setCompletedAt(Instant.now());
            logger.warn("Job {} exceeded max retries, moving to DEAD_LETTER", job.getId());
        } else {
            // Schedule retry with exponential backoff
            long delaySeconds = calculateRetryDelay(newRetryCount);
            job.setStatus(JobStatus.RETRY_SCHEDULED);
            job.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            logger.info("Job {} scheduled for retry in {} seconds (attempt {}/{})", 
                job.getId(), delaySeconds, newRetryCount, job.getMaxRetries());
        }

        jobRepository.save(job);
    }

    private long calculateRetryDelay(int retryCount) {
        // Exponential backoff: baseDelay * 2^retryCount with jitter
        long baseDelay = 5; // 5 seconds base
        long delay = baseDelay * (1L << retryCount); // 2^retryCount
        long maxDelay = 3600; // 1 hour max
        delay = Math.min(delay, maxDelay);
        
        // Add jitter: ±20%
        double jitter = (Math.random() * 0.4 - 0.2) * delay; // -20% to +20%
        return Math.max(1, (long) (delay + jitter));
    }
}
