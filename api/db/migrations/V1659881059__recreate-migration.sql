drop table if exists migration_logs;

CREATE TABLE  migration_logs (
    id BIGSERIAL,
    payment_id BIGINT NULL,
    acc_util_id BIGINT NULL,
    payment_num VARCHAR(50) NULL,
    currency varchar(10) NULL,
    currency_amount decimal(11,4) NULL,
    ledger_amount decimal(11,4) NULL,
    bank_pay_amount decimal(11,4) NULL,
    account_util_curr_amount decimal(11,4) NULL,
    account_util_led_amount decimal(11,4) NULL,
    status VARCHAR(20),
    error_message VARCHAR(5000) NULL,
    created_at timestamp
);