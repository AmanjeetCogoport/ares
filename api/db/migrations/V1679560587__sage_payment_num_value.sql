CREATE TYPE SETTLEMENT_STATUS AS ENUM (
 'CREATED',
 'POSTED',
 'POSTING_FAILED',
 'DELETED');

 CREATE CAST (varchar AS SETTLEMENT_STATUS) WITH INOUT AS IMPLICIT;

ALTER TABLE settlements ADD COLUMN settlement_status SETTLEMENT_STATUS NOT NULL DEFAULT 'CREATED';