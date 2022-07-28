CREATE TABLE IF NOT EXISTS migration_logs (
    id BIGSERIAL,
    payment_id BIGSERIAL,
    acc_util_id BIGSERIAL,
    payment_num BIGSERIAL,
    status VARCHAR(20),
    error_message VARCHAR(5000)
);