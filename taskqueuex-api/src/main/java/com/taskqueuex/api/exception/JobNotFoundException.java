package com.taskqueuex.api.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(UUID jobId) {
        super("Job not found with id: " + jobId);
    }
}
