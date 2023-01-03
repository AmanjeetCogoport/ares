CREATE TYPE public."jv_category" AS ENUM (
 'EXCH',
 'JVNOS',
 'WOFF',
 'ROFF',
 'OUTST',
 'ICJV'
 );

CREATE TYPE public."jv_status" AS ENUM (
	'PENDING',
	'APPROVED',
	'REJECTED',
	'DELETED');


CREATE CAST (varchar AS JV_CATEGORY) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS JV_STATUS) WITH INOUT AS IMPLICIT;

INSERT INTO payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
VALUES('JV',1,now(),now());


CREATE TABLE public.parent_journal_vouchers (
    id bigserial constraint parent_journal_PK primary key,
    jv_num varchar(25) not null,
    category jv_category not null,
    status jv_status not null,
    created_at timestamp not null default now(),
    created_by uuid,
    updated_at timestamp not null default now(),
    updated_by uuid
);