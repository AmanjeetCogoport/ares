ALTER TABLE payments
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE payment_invoice_mapping
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE account_utilizations
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE settlements
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

alter table audits alter column object_type type VARCHAR(100);
alter table audits alter column action_name type VARCHAR(100);

