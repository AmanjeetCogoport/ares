ALTER TABLE payments
ADD COLUMN is_suspense bool NOT NULL DEFAULT FALSE,
ADD COLUMN trade_party_document TEXT DEFAULT NULL;