CREATE TABLE PUBLIC.incident_mappings (
 	id BIGSERIAL NOT NULL,
 	account_utilization_ids INT4[],
 	data JSONB,
 	incident_type VARCHAR(25),
    incident_status VARCHAR(15),
    organization_name VARCHAR,
    entity_code INT2,
    created_by UUID,
    updated_by UUID,
 	created_at TIMESTAMP NOT NULL DEFAULT now(),
 	updated_at TIMESTAMP NOT NULL DEFAULT now()
 );