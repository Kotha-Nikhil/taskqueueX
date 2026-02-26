package com.taskqueuex.api.service;

import com.taskqueuex.api.dto.CreateJobRequest;
import com.taskqueuex.api.dto.JobResponse;
import com.taskqueuex.api.exception.DuplicateJobException;
import com.taskqueuex.api.exception.JobNotFoundException;
import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import com.taskqueuex.common.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private QueueProducerService queueProducerService;

    @Mock
    private Counter jobsSubmittedCounter;

    @Mock
    private Counter jobsRetriedCounter;

    @InjectMocks
    private JobService jobService;

    private CreateJobRequest createJobRequest;
    private Job job;

    @BeforeEach
    void setUp() {
        createJobRequest = new CreateJobRequest();
        createJobRequest.setJobType(JobType.REPORT_GENERATION);
        createJobRequest.setPayload("{\"title\":\"Test Report\",\"data\":[]}");
        createJobRequest.setPriority(0);
        createJobRequest.setMaxRetries(3);

        job = new Job();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.REPORT_GENERATION);
        job.setStatus(JobStatus.QUEUED);
        job.setPayload(createJobRequest.getPayload());
        job.setPriority(0);
        job.setMaxRetries(3);
    }

    @Test
    void submitJob_Success() {
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        doNothing().when(queueProducerService).enqueueJob(any(UUID.class));
        doNothing().when(jobsSubmittedCounter).increment();

        JobResponse response = jobService.submitJob(createJobRequest);

        assertNotNull(response);
        assertEquals(job.getId(), response.getId());
        assertEquals(JobStatus.QUEUED, response.getStatus());
        verify(jobRepository).save(any(Job.class));
        verify(queueProducerService).enqueueJob(job.getId());
        verify(jobsSubmittedCounter).increment();
    }

    @Test
    void submitJob_DuplicateIdempotencyKey_ThrowsException() {
        String idempotencyKey = "test-key";
        createJobRequest.setIdempotencyKey(idempotencyKey);
        job.setIdempotencyKey(idempotencyKey);

        when(jobRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(job));

        assertThrows(DuplicateJobException.class, () -> jobService.submitJob(createJobRequest));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void getJob_Success() {
        UUID jobId = job.getId();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobResponse response = jobService.getJob(jobId);

        assertNotNull(response);
        assertEquals(jobId, response.getId());
    }

    @Test
    void getJob_NotFound_ThrowsException() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJob(jobId));
    }

    @Test
    void retryJob_Success() {
        job.setStatus(JobStatus.FAILED);
        job.setRetryCount(1);
        job.setMaxRetries(3);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        doNothing().when(queueProducerService).enqueueJob(any(UUID.class));
        doNothing().when(jobsRetriedCounter).increment();

        JobResponse response = jobService.retryJob(job.getId());

        assertNotNull(response);
        assertEquals(JobStatus.QUEUED, job.getStatus());
        verify(queueProducerService).enqueueJob(job.getId());
        verify(jobsRetriedCounter).increment();
    }
}
