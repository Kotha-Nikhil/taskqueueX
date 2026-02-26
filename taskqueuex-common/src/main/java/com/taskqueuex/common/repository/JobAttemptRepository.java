package com.taskqueuex.common.repository;

import com.taskqueuex.common.entity.JobAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobAttemptRepository extends JpaRepository<JobAttempt, UUID> {

    List<JobAttempt> findByJobIdOrderByAttemptNumberAsc(UUID jobId);

    @Query("SELECT ja FROM JobAttempt ja WHERE ja.job.id = :jobId ORDER BY ja.attemptNumber ASC")
    List<JobAttempt> findAllByJobId(@Param("jobId") UUID jobId);
}
