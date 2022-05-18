
 ALTER SEQUENCE acc_type_master_id_seq
 AS INT
 INCREMENT  BY  1
 MINVALUE 521190   NO MAXVALUE
 START  WITH  521190
 RESTART WITH 521190
 NO  CYCLE;


 ALTER SEQUENCE account_master_acc_code_seq
 AS INT
 INCREMENT  BY  3
 RESTART WITH 10000000
 NO  CYCLE;


 ALTER SEQUENCE account_utilizations_id_seq
 AS BIGINT
 INCREMENT  BY  1
 MINVALUE 100000000   NO MAXVALUE
 START  WITH  100000000
 RESTART WITH 100000000
 NO  CYCLE;


 ALTER SEQUENCE ar_payment_files_id_seq
 AS BIGINT
 INCREMENT  BY  1
 MINVALUE 100000000   NO MAXVALUE
 START  WITH  100000000
 RESTART WITH 100000000
 NO  CYCLE;


 ALTER sequence payments_id_seq
 AS BIGINT
 INCREMENT  BY  1
 MINVALUE 100000000   NO MAXVALUE
 START  WITH  100000000
 RESTART WITH 100000000
 NO CYCLE;
