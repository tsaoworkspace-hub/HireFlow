CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id),
    round_number INTEGER NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    format VARCHAR(30) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    cancellation_reason VARCHAR(1000),
    created_by_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_interviews_round CHECK (round_number > 0),
    CONSTRAINT ck_interviews_time CHECK (ends_at > starts_at)
);

CREATE TABLE interview_interviewers (
    interview_id BIGINT NOT NULL REFERENCES interviews(id),
    interviewer_id BIGINT NOT NULL REFERENCES app_users(id),
    PRIMARY KEY (interview_id, interviewer_id)
);

CREATE TABLE interview_scorecards (
    id BIGSERIAL PRIMARY KEY,
    interview_id BIGINT NOT NULL REFERENCES interviews(id),
    interviewer_id BIGINT NOT NULL REFERENCES app_users(id),
    technical_score INTEGER NOT NULL,
    problem_analysis_score INTEGER NOT NULL,
    communication_score INTEGER NOT NULL,
    learning_ability_score INTEGER NOT NULL,
    overall_comment VARCHAR(3000) NOT NULL,
    recommendation VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMPTZ,
    unlocked_at TIMESTAMPTZ,
    unlocked_by_id BIGINT REFERENCES app_users(id),
    unlock_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_scorecards_interview_interviewer UNIQUE (interview_id, interviewer_id),
    CONSTRAINT ck_scorecards_scores CHECK (
        technical_score BETWEEN 1 AND 5
        AND problem_analysis_score BETWEEN 1 AND 5
        AND communication_score BETWEEN 1 AND 5
        AND learning_ability_score BETWEEN 1 AND 5
    )
);

CREATE INDEX idx_interviews_application ON interviews(application_id);
CREATE INDEX idx_interviews_schedule ON interviews(starts_at, ends_at);
CREATE INDEX idx_interview_interviewers_user ON interview_interviewers(interviewer_id);
CREATE INDEX idx_scorecards_interview ON interview_scorecards(interview_id);
