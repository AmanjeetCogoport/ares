insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('CLOSING',1,now(),now());

ALTER TYPE ACCOUNT_TYPE ADD VALUE 'CLOSING';