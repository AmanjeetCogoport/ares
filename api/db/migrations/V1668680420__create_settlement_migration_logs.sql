CREATE TABLE IF NOT EXISTS settlements_migration_logs (
    id BIGSERIAL,
    source_id varchar(20),
    source_value varchar(50),
    destination_id varchar(20),
    destination_value varchar(50),
    ledger_currency varchar(10),
    ledger_amount decimal(11,4),
    acc_mode varchar(40),
    status varchar(20),
    error_message varchar(5000),
    migration_date TIMESTAMP
);