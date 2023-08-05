ALTER TABLE ledger_summary
RENAME COLUMN total_on_account_amount TO total_open_on_account_amount;

ALTER TABLE ledger_summary
ADD COLUMN total_invoice_amount NUMERIC(18,4),
ADD COLUMN total_on_account_amount NUMERIC(18,4),
ADD COLUMN total_on_account_count BIGINT,
ADD COLUMN total_invoices_count BIGINT;