package com.taskqueuex.api.exception;

public class DuplicateJobException extends RuntimeException {
    public DuplicateJobException(String message) {
        super(message);
    }
}
