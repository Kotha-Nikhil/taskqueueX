package com.taskqueuex.api.controller;

import com.taskqueuex.api.dto.CreateJobRequest;
import com.taskqueuex.api.dto.JobResponse;
import com.taskqueuex.api.dto.PagedResponse;
import com.taskqueuex.api.service.JobService;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@Tag(name = "Jobs", description = "Job submission and management API")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @Operation(summary = "Submit a new job", description = "Creates a new job and enqueues it for processing")
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieves job details by its unique identifier")
    public ResponseEntity<JobResponse> getJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id) {
        JobResponse response = jobService.getJob(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List jobs", description = "Retrieves a paginated list of jobs with optional filtering")
    public ResponseEntity<PagedResponse<JobResponse>> listJobs(
            @Parameter(description = "Filter by status") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Filter by job type") @RequestParam(required = false) JobType jobType,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        PagedResponse<JobResponse> response = jobService.listJobs(status, jobType, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed job", description = "Re-enqueues a failed or dead-letter job for retry")
    public ResponseEntity<JobResponse> retryJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id) {
        JobResponse response = jobService.retryJob(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a job", description = "Cancels a queued or in-progress job")
    public ResponseEntity<JobResponse> cancelJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id) {
        JobResponse response = jobService.cancelJob(id);
        return ResponseEntity.ok(response);
    }
}
