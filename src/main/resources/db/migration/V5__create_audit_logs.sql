CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    project_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);