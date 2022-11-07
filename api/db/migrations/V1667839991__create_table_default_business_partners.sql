CREATE TABLE default_business_partners
(
    id  BIGSERIAL PRIMARY KEY,
    business_name    VARCHAR(100) NOT NULL,
    trade_party_detail_serial_id  VARCHAR(20) NOT NULL,
    sage_org_id   VARCHAR(20) ,
    trade_party_detail_id  UUID NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP
);