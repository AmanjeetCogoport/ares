INSERT INTO payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
VALUES('CTDS',1,now(),now());

INSERT INTO payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
VALUES('CTDSP',1,now(),now());

INSERT INTO payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
VALUES('VTDS',1,now(),now());

ALTER TYPE ACCOUNT_TYPE ADD VALUE IF NOT EXISTS 'CTDSP';
ALTER TYPE SETTLEMENT_TYPE ADD VALUE IF NOT EXISTS 'CTDSP';
ALTER TYPE PAYMENT_CODE ADD VALUE IF NOT EXISTS 'CTDSP';