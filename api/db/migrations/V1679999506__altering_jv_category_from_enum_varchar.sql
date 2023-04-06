ALTER TABLE journal_vouchers ADD COLUMN category_varchar VARCHAR;

UPDATE journal_vouchers SET category_varchar = category::text;
ALTER TABLE journal_vouchers DROP COLUMN category;
ALTER TABLE journal_vouchers RENAME COLUMN category_varchar TO category;