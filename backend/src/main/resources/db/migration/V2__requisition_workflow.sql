CREATE TABLE job_requisitions (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    job_title VARCHAR(120),
    headcount INTEGER,
    reason VARCHAR(1000),
    required_skills VARCHAR(2000),
    salary_min NUMERIC(12, 2),
    salary_max NUMERIC(12, 2),
    target_start_date DATE,
    status VARCHAR(30) NOT NULL,
    created_by_id BIGINT NOT NULL REFERENCES app_users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT ck_job_requisitions_headcount CHECK (headcount IS NULL OR headcount > 0),
    CONSTRAINT ck_job_requisitions_salary_min CHECK (salary_min IS NULL OR salary_min >= 0),
    CONSTRAINT ck_job_requisitions_salary_max CHECK (salary_max IS NULL OR salary_max >= 0),
    CONSTRAINT ck_job_requisitions_salary_range CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_max >= salary_min
    )
);

CREATE TABLE requisition_actions (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES job_requisitions(id),
    action_type VARCHAR(30) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    actor_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_openings (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL UNIQUE REFERENCES job_requisitions(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    job_title VARCHAR(120) NOT NULL,
    headcount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    responsible_hr_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    CONSTRAINT ck_job_openings_headcount CHECK (headcount > 0)
);

CREATE INDEX idx_job_requisitions_status ON job_requisitions(status);
CREATE INDEX idx_job_requisitions_department ON job_requisitions(department_id);
CREATE INDEX idx_job_requisitions_created_by ON job_requisitions(created_by_id);
CREATE INDEX idx_requisition_actions_requisition ON requisition_actions(requisition_id, created_at);
CREATE INDEX idx_job_openings_status ON job_openings(status);
