CREATE TABLE bpr
(
    id   BIGSERIAL PRIMARY KEY,
    business_name    VARCHAR(100) NOT NULL,
    trade_party_detail_serial_id  BIGINT NOT NULL,
    sage_org_id   VARCHAR(20) ,
    trade_party_detail_id  UUID NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP
);