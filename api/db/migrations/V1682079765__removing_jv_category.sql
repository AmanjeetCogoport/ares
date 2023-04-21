DELETE FROM journal_voucher_categories where category ='CSINV';
DELETE FROM journal_voucher_categories where category ='ZSINV';
DELETE FROM journal_voucher_categories where category ='CSMEM';
DELETE FROM journal_voucher_categories where category ='ZSMEM';
DELETE FROM journal_voucher_categories where category ='SPMEM';

Insert into journal_voucher_categories (category, description)
VALUES ('SINV', 'Customer Invoice'),
 ('PINV', 'Purchase Invoice'),
 ('PCN', 'Purchase Credit Note'),
 ('SCN', 'Sales Credit Note');
