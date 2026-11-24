ALTER TABLE job_openings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE candidates (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    current_position VARCHAR(120),
    years_experience NUMERIC(4, 1),
    skills VARCHAR(2000),
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_candidates_years_experience CHECK (years_experience IS NULL OR years_experience >= 0)
);

CREATE UNIQUE INDEX uk_candidates_email_lower ON candidates (LOWER(email));

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL REFERENCES candidates(id),
    job_opening_id BIGINT NOT NULL REFERENCES job_openings(id),
    stage VARCHAR(40) NOT NULL,
    created_by_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_applications_candidate_opening UNIQUE (candidate_id, job_opening_id)
);

CREATE TABLE application_stage_histories (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id),
    from_stage VARCHAR(40),
    to_stage VARCHAR(40) NOT NULL,
    reason VARCHAR(1000),
    actor_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_applications_opening ON applications(job_opening_id);
CREATE INDEX idx_applications_stage ON applications(stage);
CREATE INDEX idx_application_histories_application ON application_stage_histories(application_id, created_at);
