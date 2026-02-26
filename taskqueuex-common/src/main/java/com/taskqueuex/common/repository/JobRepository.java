package com.taskqueuex.common.repository;

import com.taskqueuex.common.entity.Job;
import com.taskqueuex.common.enums.JobStatus;
import com.taskqueuex.common.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByJobType(JobType jobType, Pageable pageable);

    Page<Job> findByStatusAndJobType(JobStatus status, JobType jobType, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.nextRetryAt <= :now")
    List<Job> findJobsReadyForRetry(@Param("status") JobStatus status, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Job j SET j.status = :status, j.workerId = :workerId, j.startedAt = :startedAt, j.version = j.version + 1 " +
           "WHERE j.id = :id AND j.status = :expectedStatus AND j.version = :version")
    int claimJob(@Param("id") UUID id, 
                 @Param("status") JobStatus status,
                 @Param("workerId") String workerId,
                 @Param("startedAt") Instant startedAt,
                 @Param("expectedStatus") JobStatus expectedStatus,
                 @Param("version") Long version);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = :status")
    long countByStatus(@Param("status") JobStatus status);
}
