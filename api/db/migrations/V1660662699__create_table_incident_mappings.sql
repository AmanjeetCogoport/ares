CREATE TYPE PUBLIC.incident_type AS ENUM (
    'SETTLEMENT_APPROVAL'
);

CREATE CAST (varchar AS incident_type) WITH INOUT AS IMPLICIT;

CREATE TYPE PUBLIC.incident_status AS ENUM (
    'REQUESTED',
    'REJECTED',
    'APPROVED'
);

CREATE CAST (varchar AS incident_status) WITH INOUT AS IMPLICIT;

CREATE TABLE PUBLIC.incident_mappings (
 	id BIGSERIAL NOT NULL,
 	account_utilization_ids JSONB,
 	data JSONB,
 	incident_type incident_type NOT NULL,
    incident_status incident_status NOT NULL,
    organization_name VARCHAR,
    entity_code INT2,
    created_by UUID,
    updated_by UUID,
 	created_at TIMESTAMP NOT NULL DEFAULT now(),
 	updated_at TIMESTAMP NOT NULL DEFAULT now()
 );