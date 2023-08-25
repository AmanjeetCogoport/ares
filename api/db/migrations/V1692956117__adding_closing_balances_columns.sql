ALTER TABLE ledger_summary
ADD COLUMN closing_invoice_balance2022 NUMERIC(18, 4),
ADD COLUMN closing_on_account_balance2022 NUMERIC(18, 4),
ADD COLUMN closing_outstanding2022 NUMERIC(18, 4);
