CREATE TABLE ares_documents (
    id BIGSERIAL PRIMARY KEY,
    document_url text,
    document_name VARCHAR(250),
    document_type VARCHAR(50),
    uploaded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
