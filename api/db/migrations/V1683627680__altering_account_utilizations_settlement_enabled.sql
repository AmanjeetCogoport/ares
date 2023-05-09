ALTER TABLE account_utilizations
ADD COLUMN settlement_enabled bool NOT NULL DEFAULT false;

UPDATE account_utilizations SET
settlement_enabled = true WHERE document_status = 'FINAL' AND deleted_at is null