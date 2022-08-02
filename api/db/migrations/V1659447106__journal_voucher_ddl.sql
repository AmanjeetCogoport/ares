CREATE TYPE public."jv_category" AS ENUM ( 'EXCH', 'NOSTRO', 'WOFF', 'ROFF', 'OUTST' );

CREATE CAST (varchar AS jv_category) WITH INOUT AS IMPLICIT;

create table public.journal_vouchers (
    id bigserial constraint settlement_PK primary key,
    entity_id uuid default NULL,
    entity_code int2 default NULL,
    jv_num varchar(25) not null,
    type varchar(10),
    category jv_category,
    validity_date date,
    currency varchar(5),
    amount decimal(13,4),
    trade_party_id uuid NULL,
    trade_partner_name varchar(200) NULL,
    created_at timestamp not null default now(),
    created_by uuid,
    updated_at timestamp not null default now(),
    updated_by uuid
);