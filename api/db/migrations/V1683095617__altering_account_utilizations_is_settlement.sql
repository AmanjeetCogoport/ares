ALTER TABLE account_utilizations
ADD COLUMN is_settlement bool NOT NULL DEFAULT false;