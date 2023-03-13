ALTER TABLE account_utilizations
ADD COLUMN tagged_settlement_id TEXT;

ALTER TABLE account_utilizations
ADD COLUMN is_draft bool NOT NULL DEFAULT false;

ALTER TABLE settlements
ADD COLUMN is_draft bool NOT NULL DEFAULT false;