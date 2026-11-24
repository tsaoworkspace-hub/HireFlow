CREATE TABLE google_calendar_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    access_token_encrypted VARCHAR(4000) NOT NULL,
    refresh_token_encrypted VARCHAR(4000),
    access_token_expires_at TIMESTAMPTZ NOT NULL,
    granted_scope VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE google_calendar_event_links (
    id BIGSERIAL PRIMARY KEY,
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    google_event_id VARCHAR(500) NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_google_event_interview_user UNIQUE (interview_id, user_id)
);

CREATE TABLE calendar_sync_jobs (
    id BIGSERIAL PRIMARY KEY,
    deduplication_key VARCHAR(180) NOT NULL UNIQUE,
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    operation VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_calendar_sync_jobs_due ON calendar_sync_jobs(status, next_retry_at, created_at);
