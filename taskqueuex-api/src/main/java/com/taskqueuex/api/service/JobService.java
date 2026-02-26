package com.taskqueuex.api.service;

import com.taskqueuex.api.dto.CreateJobRequest;
import com.taskqueuex.api.dto.JobResponse;
import com.taskqueuex.api.dto.PagedResponse;
import com.taskqueuex.api.exception.DuplicateJobException;
import com.taskqueuex.api.exception.JobNotFoundException;
import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import com.taskqueuex.common.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final QueueProducerService queueProducerService;
    private final Counter jobsSubmittedCounter;
    private final Counter jobsRetriedCounter;

    public JobService(
            JobRepository jobRepository,
            QueueProducerService queueProducerService,
            MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.queueProducerService = queueProducerService;
        this.jobsSubmittedCounter = Counter.builder("jobs.submitted")
            .description("Total number of jobs submitted")
            .register(meterRegistry);
        this.jobsRetriedCounter = Counter.builder("jobs.retried")
            .description("Total number of job retries")
            .register(meterRegistry);
    }

    @Transactional
    public JobResponse submitJob(CreateJobRequest request) {
        // Check for duplicate idempotency key
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            jobRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existingJob -> {
                    throw new DuplicateJobException(
                        "Job with idempotency key '" + request.getIdempotencyKey() + "' already exists with id: " + existingJob.getId()
                    );
                });
        }

        // Create job entity
        Job job = new Job();
        job.setJobType(request.getJobType());
        job.setStatus(JobStatus.QUEUED);
        job.setPayload(request.getPayload());
        job.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        job.setIdempotencyKey(request.getIdempotencyKey());
        job.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        job.setCreatedAt(Instant.now());

        job = jobRepository.save(job);
        logger.info("Created job with id: {}", job.getId());

        // Push to queue
        queueProducerService.enqueueJob(job.getId());

        // Increment metrics
        jobsSubmittedCounter.increment();

        return mapToResponse(job);
    }

    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
        return mapToResponse(job);
    }

    public PagedResponse<JobResponse> listJobs(JobStatus status, JobType jobType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> jobPage;

        if (status != null && jobType != null) {
            jobPage = jobRepository.findByStatusAndJobType(status, jobType, pageable);
        } else if (status != null) {
            jobPage = jobRepository.findByStatus(status, pageable);
        } else if (jobType != null) {
            jobPage = jobRepository.findByJobType(jobType, pageable);
        } else {
            jobPage = jobRepository.findAll(pageable);
        }

        return new PagedResponse<>(
            jobPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()),
            jobPage.getNumber(),
            jobPage.getSize(),
            jobPage.getTotalElements()
        );
    }

    @Transactional
    public JobResponse retryJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() != JobStatus.FAILED && job.getStatus() != JobStatus.DEAD_LETTER) {
            throw new IllegalArgumentException("Job cannot be retried. Current status: " + job.getStatus());
        }

        if (job.getRetryCount() >= job.getMaxRetries()) {
            throw new IllegalArgumentException("Job has exceeded max retries");
        }

        job.setStatus(JobStatus.QUEUED);
        job.setLastError(null);
        job.setNextRetryAt(null);
        job = jobRepository.save(job);

        logger.info("Retrying job with id: {}", jobId);
        queueProducerService.enqueueJob(job.getId());

        // Increment metrics
        jobsRetriedCounter.increment();

        return mapToResponse(job);
    }

    @Transactional
    public JobResponse cancelJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() == JobStatus.SUCCEEDED || job.getStatus() == JobStatus.CANCELLED) {
            throw new IllegalArgumentException("Job cannot be cancelled. Current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        job = jobRepository.save(job);

        logger.info("Cancelled job with id: {}", jobId);
        return mapToResponse(job);
    }

    private JobResponse mapToResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setJobType(job.getJobType());
        response.setStatus(job.getStatus());
        response.setPayload(job.getPayload());
        response.setPriority(job.getPriority());
        response.setIdempotencyKey(job.getIdempotencyKey());
        response.setCreatedAt(job.getCreatedAt());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setRetryCount(job.getRetryCount());
        response.setMaxRetries(job.getMaxRetries());
        response.setLastError(job.getLastError());
        response.setOutputLocation(job.getOutputLocation());
        response.setWorkerId(job.getWorkerId());
        response.setNextRetryAt(job.getNextRetryAt());
        return response;
    }
}
