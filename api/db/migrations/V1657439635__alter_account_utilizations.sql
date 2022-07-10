ALTER TABLE account_utilizations
ADD COLUMN taxable_amount numeric(13, 4) NOT null default 0;