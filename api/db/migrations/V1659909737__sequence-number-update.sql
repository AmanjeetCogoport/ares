update payment_sequence_numbers
set next_sequence_number=100000000000,updated_at=now() where sequence_type='REC';

update payment_sequence_numbers
set next_sequence_number=100000000000,updated_at=now() where sequence_type='PAY';