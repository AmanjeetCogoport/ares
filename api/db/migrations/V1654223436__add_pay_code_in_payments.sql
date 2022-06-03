-- PAY = payment given to vendor , REC = received from invoicing party
-- CTDS = customer TDS , VTDS = vendor TDS

CREATE TYPE public."payment_code" AS ENUM (
	'PAY',
	'REC',
	'CTDS',
	'VTDS');

CREATE CAST (varchar AS PAYMENT_CODE) WITH INOUT AS IMPLICIT;

create table public."payment_invoice_mapping"(
 id bigserial constraint pim_PK primary key,
 account_mode ACCOUNT_MODE,
 document_no bigint,
 payment_id bigint constraint payment_id_FK references payments(id),
 mapping_type varchar(10) check(mapping_type in('TDS','INVOICE','BILL')),
 currency varchar(5),
 sign_flag smallint,
 amount decimal(13,4) ,
 led_currency varchar(5),
 led_amount decimal(13,4),
 transaction_date date,
 created_at timestamp not null default now(),
 updated_at timestamp not null default now()
);

alter table payments
add payment_code PAYMENT_CODE;