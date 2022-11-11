ALTER TABLE payments
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE payment_invoice_mapping
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE account_utilizations
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE settlements
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER table audits ALTER COLUMN object_type type VARCHAR(100);
ALTER table audits ALTER COLUMN action_name type VARCHAR(100);