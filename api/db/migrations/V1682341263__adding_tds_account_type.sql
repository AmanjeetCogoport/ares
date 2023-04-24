insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('CTDS',1,now(),now());

insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('CTDSP',1,now(),now());

insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('VTDS',1,now(),now());


ALTER TYPE ACCOUNT_TYPE ADD VALUE 'VTDS';
ALTER TYPE ACCOUNT_TYPE ADD VALUE 'CTDS';
ALTER TYPE ACCOUNT_TYPE ADD VALUE 'CTDSP';

ALTER TYPE SETTLEMENT_TYPE ADD VALUE 'VTDS';
ALTER TYPE SETTLEMENT_TYPE ADD VALUE 'CTDS';
ALTER TYPE SETTLEMENT_TYPE ADD VALUE 'CTDSP';

ALTER TYPE PAYMENT_CODE ADD VALUE 'CTDSP';