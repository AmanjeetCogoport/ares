ALTER TABLE payment_invoice_mapping
ADD CONSTRAINT journal_vouchers_fk
FOREIGN KEY (journal_voucher_id)
REFERENCES journal_vouchers(id);