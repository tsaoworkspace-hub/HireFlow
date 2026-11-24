ALTER TABLE job_openings ADD COLUMN public_slug VARCHAR(180);
ALTER TABLE job_openings ADD COLUMN public_description VARCHAR(5000);
ALTER TABLE job_openings ADD COLUMN location VARCHAR(120);
ALTER TABLE job_openings ADD COLUMN employment_type VARCHAR(60);
ALTER TABLE job_openings ADD COLUMN published_at TIMESTAMPTZ;

UPDATE job_openings
SET public_slug = 'job-' || id,
    public_description = COALESCE((SELECT reason FROM job_requisitions WHERE id = job_openings.requisition_id), job_title),
    employment_type = '全職',
    published_at = CASE WHEN status = 'OPEN' THEN created_at ELSE NULL END;

ALTER TABLE job_openings ALTER COLUMN public_slug SET NOT NULL;
CREATE UNIQUE INDEX uk_job_openings_public_slug ON job_openings(public_slug);

ALTER TABLE applications ADD COLUMN public_id UUID;
UPDATE applications SET public_id = gen_random_uuid();
ALTER TABLE applications ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX uk_applications_public_id ON applications(public_id);

CREATE TABLE candidate_profiles (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL UNIQUE REFERENCES candidates(id),
    address VARCHAR(300),
    autobiography VARCHAR(5000),
    portfolio_url VARCHAR(500),
    consented_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE work_experiences (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    company_name VARCHAR(150) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    start_date DATE,
    end_date DATE,
    current_job BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(2000),
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_work_experiences_candidate ON work_experiences(candidate_id, sort_order);

CREATE TABLE applicant_access_tokens (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    last_accessed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
