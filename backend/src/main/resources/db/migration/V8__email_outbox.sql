CREATE TABLE email_deliveries (
    id BIGSERIAL PRIMARY KEY,
    deduplication_key VARCHAR(180) NOT NULL UNIQUE,
    message_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    text_body VARCHAR(8000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_email_deliveries_due ON email_deliveries(status, next_retry_at, created_at);

