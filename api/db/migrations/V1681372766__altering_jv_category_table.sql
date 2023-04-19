ALTER TABLE journal_voucher_categories DROP COLUMN default_journal_code;
ALTER TABLE journal_voucher_categories DROP COLUMN entity_code;
ALTER TABLE journal_voucher_categories DROP COLUMN country_code;
ALTER TABLE journal_voucher_categories DROP COLUMN valid_start;
ALTER TABLE journal_voucher_categories DROP COLUMN valid_end;
ALTER TABLE journal_voucher_categories DROP COLUMN created_by;
ALTER TABLE journal_voucher_categories DROP COLUMN updated_by;

ALTER TABLE journal_voucher_codes DROP COLUMN entity_code;
ALTER TABLE journal_voucher_codes DROP COLUMN country_code;
ALTER TABLE journal_voucher_codes DROP COLUMN jv_category_id;
ALTER TABLE journal_voucher_codes DROP COLUMN created_by;
ALTER TABLE journal_voucher_codes DROP COLUMN updated_by;
