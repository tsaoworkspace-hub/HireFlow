CREATE TABLE interview_time_slots (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    round_number INTEGER NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    format VARCHAR(30) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    proposed_by_id BIGINT NOT NULL REFERENCES app_users(id),
    selected_at TIMESTAMPTZ,
    interview_id BIGINT UNIQUE REFERENCES interviews(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_interview_slots_round CHECK (round_number > 0),
    CONSTRAINT ck_interview_slots_time CHECK (ends_at > starts_at),
    CONSTRAINT uk_interview_slots_application_time UNIQUE (application_id, starts_at, ends_at)
);

CREATE TABLE interview_time_slot_interviewers (
    slot_id BIGINT NOT NULL REFERENCES interview_time_slots(id) ON DELETE CASCADE,
    interviewer_id BIGINT NOT NULL REFERENCES app_users(id),
    PRIMARY KEY (slot_id, interviewer_id)
);

CREATE INDEX idx_interview_slots_application ON interview_time_slots(application_id, status);
CREATE INDEX idx_interview_slots_department_time ON interview_time_slots(department_id, starts_at);

