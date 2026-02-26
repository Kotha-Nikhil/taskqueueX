package com.taskqueuex.common.enums;

public enum JobStatus {
    QUEUED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    CANCELLED
}
