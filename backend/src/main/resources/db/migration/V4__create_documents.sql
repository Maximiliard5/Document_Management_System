CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    size BIGINT NOT NULL,
    minio_key VARCHAR(512) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);