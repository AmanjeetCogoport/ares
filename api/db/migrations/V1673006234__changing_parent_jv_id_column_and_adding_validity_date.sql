ALTER TABLE parent_journal_vouchers ADD COLUMN validity_date DATE;
ALTER TABLE journal_vouchers ALTER COLUMN parent_jv_id type BIGINT USING parent_jv_id::BIGINT;