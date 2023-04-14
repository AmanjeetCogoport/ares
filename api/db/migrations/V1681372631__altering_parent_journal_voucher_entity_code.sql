ALTER TABLE parent_journal_vouchers
ADD COLUMN entity_code int2 DEFAULT NULL,
ADD COLUMN transaction_date DATE DEFAULT NULL,
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE parent_journal_vouchers DROP COLUMN amount;
ALTER TABLE parent_journal_vouchers DROP COLUMN acc_mode;

ALTER TABLE journal_vouchers
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;