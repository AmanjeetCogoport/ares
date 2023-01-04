INSERT INTO payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
VALUES('JV',1,now(),now());


CREATE TABLE public.parent_journal_vouchers (
    id BIGSERIAL CONSTRAINT parent_journal_PK PRIMARY KEY,
    jv_num VARCHAR(25) NOT NULL,
    category JV_CATEGORY NOT NULL,
    status JV_STATUS NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by UUID
);