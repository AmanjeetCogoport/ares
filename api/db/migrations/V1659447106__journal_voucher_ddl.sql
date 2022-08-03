CREATE TYPE public."jv_category" AS ENUM (
 'EXCH',
 'NOSTRO',
 'WOFF',
 'ROFF',
 'OUTST');

CREATE TYPE public."jv_status" AS ENUM (
	'PENDING',
	'APPROVED',
	'REJECTED',
	'DELETED');


CREATE CAST (varchar AS JV_CATEGORY) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS JV_STATUS) WITH INOUT AS IMPLICIT;

create table public.journal_vouchers (
    id bigserial constraint journal_PK primary key,
    entity_id uuid default NULL,
    entity_code int2 default NULL,
    jv_num varchar(25) not null,
    type varchar(10),
    category jv_category not null,
    validity_date date,
    currency varchar(5),
    amount decimal(13,4),
    status jv_status not null,
    exchange_rate decimal(9,4),
    trade_party_id uuid NULL,
    trade_partner_name varchar(200) NULL,
    created_at timestamp not null default now(),
    created_by uuid,
    updated_at timestamp not null default now(),
    updated_by uuid
);