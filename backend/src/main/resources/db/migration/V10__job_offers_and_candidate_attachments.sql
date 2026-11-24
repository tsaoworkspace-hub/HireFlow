CREATE TABLE job_offers (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES applications(id),
    status VARCHAR(30) NOT NULL,
    annual_salary NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    proposed_start_date DATE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(5000) NOT NULL,
    created_by_id BIGINT NOT NULL REFERENCES app_users(id),
    approved_by_id BIGINT REFERENCES app_users(id),
    sent_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    response_reason VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_job_offers_salary CHECK (annual_salary > 0)
);

CREATE TABLE job_offer_histories (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL REFERENCES job_offers(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(2000),
    actor_id BIGINT REFERENCES app_users(id),
    actor_type VARCHAR(20) NOT NULL,
    actor_label VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE candidate_attachments (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_candidate_attachments_size CHECK (file_size > 0)
);

CREATE INDEX idx_job_offers_status ON job_offers(status, expires_at);
CREATE INDEX idx_job_offer_histories_offer ON job_offer_histories(offer_id, created_at);
CREATE INDEX idx_candidate_attachments_application ON candidate_attachments(application_id, active, created_at);

ALTER TABLE applicant_access_tokens DROP CONSTRAINT IF EXISTS applicant_access_tokens_application_id_key;
CREATE INDEX idx_applicant_tokens_application ON applicant_access_tokens(application_id, expires_at);
