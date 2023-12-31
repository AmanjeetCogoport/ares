CREATE TYPE public."settlement_type" AS ENUM (
        'SINV',
        'PINV',
        'SCN',
        'SDN',
        'PCN',
        'PDN',
        'REC',
        'PAY',
        'SECH',
        'PECH',
        'CTDS',
        'VTDS');

CREATE CAST (varchar AS SETTLEMENT_TYPE) WITH INOUT AS IMPLICIT;

create table public.settlements(
    id bigserial constraint settlement_PK primary key,
    source_id bigint,
    source_type SETTLEMENT_TYPE,
    destination_id bigint not null,
    destination_type SETTLEMENT_TYPE not null,
    currency varchar(5),
    amount decimal(13,4),
    led_currency varchar(5) not null,
    led_amount decimal(13,4) not null,
    sign_flag smallint not null,
    settlement_date date not null,
    created_at timestamp not null default now(),
    created_by uuid,
    updated_at timestamp not null default now(),
    updated_by uuid
);