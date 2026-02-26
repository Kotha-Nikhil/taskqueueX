package com.taskqueuex.api.dto;

import com.taskqueuex.common.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class CreateJobRequest {

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotBlank(message = "Payload is required")
    private String payload;

    @Min(value = 0, message = "Priority must be >= 0")
    @Max(value = 10, message = "Priority must be <= 10")
    private Integer priority = 0;

    private String idempotencyKey;

    @Min(value = 0, message = "Max retries must be >= 0")
    @Max(value = 10, message = "Max retries must be <= 10")
    private Integer maxRetries = 3;

    // Getters and Setters
    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}
