package com.taskqueuex.api.integration;

import com.taskqueuex.api.dto.CreateJobRequest;
import com.taskqueuex.api.dto.JobResponse;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import com.taskqueuex.common.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class JobIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("minio.endpoint", () -> "http://localhost:9000");
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("rate-limit.enabled", () -> "false");
    }

    @Test
    void submitAndGetJob_Success() {
        CreateJobRequest request = new CreateJobRequest();
        request.setJobType(JobType.REPORT_GENERATION);
        request.setPayload("{\"title\":\"Integration Test Report\",\"data\":[{\"name\":\"Test\",\"value\":\"123\"}]}");
        request.setPriority(0);

        ResponseEntity<JobResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/jobs",
            request,
            JobResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());
        assertEquals(JobStatus.QUEUED, createResponse.getBody().getStatus());

        // Get the job
        ResponseEntity<JobResponse> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/jobs/" + createResponse.getBody().getId(),
            JobResponse.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(createResponse.getBody().getId(), getResponse.getBody().getId());
    }
}
