create table public.”settlements”(
    id bigserial constraint pim_PK primary key,
    account_mode ACCOUNT_MODE,
    source_id bigint,
    source_type ACCOUNT_TYPE,
    destination_id bigint,
    destination_type ACCOUNT_TYPE,
    currency varchar(5),
    amount decimal(13,4) ,
    led_currency varchar(5),
    led_amount decimal(13,4),
    settlement_date date,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);