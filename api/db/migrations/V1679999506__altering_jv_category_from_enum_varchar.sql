UPDATE journal_vouchers SET category_varchar = category::varchar;
ALTER TABLE journal_vouchers DROP COLUMN category;
ALTER TABLE journal_vouchers RENAME COLUMN category_varchar TO category;