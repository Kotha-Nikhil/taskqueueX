package com.taskqueuex.worker.service;

import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobClaimService {

    private static final Logger logger = LoggerFactory.getLogger(JobClaimService.class);

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<Job> claimJob(UUID jobId, String workerId) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            logger.warn("Job {} not found for claiming", jobId);
            return Optional.empty();
        }

        Job job = jobOpt.get();
        if (job.getStatus() != JobStatus.QUEUED && job.getStatus() != JobStatus.RETRY_SCHEDULED) {
            logger.debug("Job {} is not in claimable state. Current status: {}", jobId, job.getStatus());
            return Optional.empty();
        }

        int updated = jobRepository.claimJob(
            jobId,
            JobStatus.IN_PROGRESS,
            workerId,
            Instant.now(),
            job.getStatus(),
            job.getVersion()
        );

        if (updated == 0) {
            logger.debug("Failed to claim job {}. Another worker may have claimed it.", jobId);
            return Optional.empty();
        }

        // Reload to get updated version
        job = jobRepository.findById(jobId).orElseThrow();
        logger.info("Successfully claimed job {} by worker {}", jobId, workerId);
        return Optional.of(job);
    }
}
