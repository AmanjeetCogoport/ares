ALTER TABLE account_utilizations
ADD COLUMN settlement_enabled bool NOT NULL DEFAULT false;