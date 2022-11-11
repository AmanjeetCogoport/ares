CREATE TABLE defaulted_business_partners
(
    id  BIGSERIAL PRIMARY KEY,
    business_name    VARCHAR(100) NOT NULL,
    trade_party_detail_serial_id  BIGSERIAL NOT NULL UNIQUE,
    sage_org_id   VARCHAR(20) NOT NULL UNIQUE,
    trade_party_detail_id  UUID NOT NULL UNIQUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP
);