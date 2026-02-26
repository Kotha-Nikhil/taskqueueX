CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    output_location VARCHAR(500),
    worker_id VARCHAR(100),
    next_retry_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE UNIQUE INDEX idx_jobs_idempotency_key ON jobs(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_jobs_job_type ON jobs(job_type);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
CREATE INDEX idx_jobs_next_retry_at ON jobs(next_retry_at) WHERE next_retry_at IS NOT NULL;
