ALTER TABLE ledger_summary
DROP CONSTRAINT unique_created_at_organization_id,
ADD CONSTRAINT unique_created_at_organization_id_entity_code UNIQUE (created_at, organization_id, entity_code)