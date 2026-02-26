package com.taskqueuex.worker.handler;

import com.taskqueuex.common.entity.Job;

public interface JobHandler {
    void handle(Job job) throws Exception;
    String getJobType();
}
