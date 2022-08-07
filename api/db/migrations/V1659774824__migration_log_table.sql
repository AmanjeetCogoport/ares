CREATE TABLE IF NOT EXISTS migration_logs (
    id BIGSERIAL,
    payment_id BIGSERIAL,
    acc_util_id BIGSERIAL,
    payment_num VARCHAR(50),
    currency varchar(10),
    currency_amount decimal(11,4),
    ledger_amount decimal(11,4),
    bank_pay_amount decimal(11,4),
    account_util_curr_amount decimal(11,4),
    account_util_led_amount decimal(11,4),
    status VARCHAR(20),
    error_message VARCHAR(5000),
    created_at timestamp
);

alter table payments
add bank_pay_amount decimal(11,4);

ALTER TYPE public."account_type" ADD VALUE 'OPDIV';
ALTER TYPE public."account_type" ADD VALUE 'MISC';
ALTER TYPE public."account_type" ADD VALUE 'BANK';
ALTER TYPE public."account_type" ADD VALUE 'CONTR';
ALTER TYPE public."account_type" ADD VALUE 'INTER';
ALTER TYPE public."account_type" ADD VALUE 'MTC';
ALTER TYPE public."account_type" ADD VALUE 'MTCCV';

ALTER TYPE public."payment_code" ADD VALUE "APRE";
ALTER TYPE public."payment_code" ADD VALUE "CPRE";