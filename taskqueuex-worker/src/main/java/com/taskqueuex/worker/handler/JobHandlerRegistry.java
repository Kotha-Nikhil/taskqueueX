package com.taskqueuex.worker.handler;

import com.taskqueuex.common.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JobHandlerRegistry {

    private final Map<JobType, JobHandler> handlers = new HashMap<>();

    public JobHandlerRegistry(List<JobHandler> handlerList) {
        for (JobHandler handler : handlerList) {
            JobType jobType = JobType.valueOf(handler.getJobType());
            handlers.put(jobType, handler);
        }
    }

    public JobHandler getHandler(JobType jobType) {
        JobHandler handler = handlers.get(jobType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for job type: " + jobType);
        }
        return handler;
    }
}
