package com.taskqueuex.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueuex.api.dto.CreateJobRequest;
import com.taskqueuex.api.dto.JobResponse;
import com.taskqueuex.api.service.JobService;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitJob_Success() throws Exception {
        CreateJobRequest request = new CreateJobRequest();
        request.setJobType(JobType.REPORT_GENERATION);
        request.setPayload("{\"title\":\"Test Report\"}");
        request.setPriority(0);

        JobResponse response = new JobResponse();
        response.setId(UUID.randomUUID());
        response.setJobType(JobType.REPORT_GENERATION);
        response.setStatus(JobStatus.QUEUED);
        response.setCreatedAt(Instant.now());

        when(jobService.submitJob(any(CreateJobRequest.class))).thenReturn(response);

        mockMvc.perform(post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void getJob_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobResponse response = new JobResponse();
        response.setId(jobId);
        response.setJobType(JobType.REPORT_GENERATION);
        response.setStatus(JobStatus.SUCCEEDED);

        when(jobService.getJob(jobId)).thenReturn(response);

        mockMvc.perform(get("/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }
}
