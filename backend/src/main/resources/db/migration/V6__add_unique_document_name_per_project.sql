ALTER TABLE documents ADD CONSTRAINT uq_document_name_per_project UNIQUE (project_id, name);
