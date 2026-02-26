CREATE TABLE job_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    worker_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT,
    CONSTRAINT fk_job_attempts_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);

CREATE INDEX idx_job_attempts_job_id ON job_attempts(job_id);
CREATE INDEX idx_job_attempts_started_at ON job_attempts(started_at);
