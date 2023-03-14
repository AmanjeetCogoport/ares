ALTER TABLE parent_journal_vouchers
ADD COLUMN  currency varchar(5),
ADD COLUMN  led_currency varchar(5),
ADD COLUMN  amount decimal(13,4),
ADD COLUMN  exchange_rate decimal(9,4),
ADD COLUMN  acc_mode ACCOUNT_MODE,
ADD COLUMN  description varchar;

ALTER TABLE journal_vouchers
ADD COLUMN gl_code varchar;

ALTER TABLE journal_vouchers ALTER COLUMN acc_mode DROP NOT NULL;
