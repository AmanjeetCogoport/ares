ALTER TYPE JV_STATUS ADD VALUE 'UTILIZED';
ALTER TABLE journal_vouchers ADD COLUMN acc_mode public."account_mode" NOT NULL;
