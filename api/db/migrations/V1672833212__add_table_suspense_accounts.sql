CREATE TABLE suspense_accounts (
    id              BIGSERIAL     NOT NULL PRIMARY KEY,
    payment_id      BIGINT     DEFAULT NULL,
    entity_code     INT           NOT NULL,
    currency        VARCHAR(10)   NOT NULL,
    amount          NUMERIC(13,4) NOT NULL DEFAULT 0,
    exchange_rate   DECIMAL(9,4),
    led_currency    VARCHAR(10)   NOT NULL,
    led_amount      NUMERIC(13,4) NOT NULL DEFAULT 0,
    narration       VARCHAR(200)  DEFAULT NULL,
    bank_name       VARCHAR(100),
    cogo_account_no VARCHAR(30),
    ref_account_no  VARCHAR(30),
    ref_payment_id  VARCHAR(30),
    bank_id         UUID,
    trans_ref_number VARCHAR(50),
    transaction_date TIMESTAMP,
    is_deleted       BOOL         NOT NULL DEFAULT FALSE,
	pay_mode         PAYMENT_MODE NULL,
	created_by       UUID         NOT NULL,
	updated_by       UUID         NOT NULL,
    uploaded_by      VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP    DEFAULT  NULL,
    trade_party_document TEXT     DEFAULT NULL
);
