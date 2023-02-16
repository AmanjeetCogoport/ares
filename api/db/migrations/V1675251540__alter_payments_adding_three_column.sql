CREATE TYPE PAYMENT_DOCUMENT_STATUS AS ENUM (
 'CREATED',
 'APPROVED',
 'POSTED',
 'POSTING_FAILED');

CREATE CAST (varchar AS PAYMENT_DOCUMENT_STATUS) WITH INOUT AS IMPLICIT;

ALTER TABLE payments
ADD COLUMN payment_document_status PAYMENT_DOCUMENT_STATUS NOT NULL DEFAULT 'CREATED';

ALTER TABLE payments
ADD COLUMN created_by UUID DEFAULT NULL;

ALTER TABLE payments
ADD COLUMN updated_by UUID DEFAULT NULL;
