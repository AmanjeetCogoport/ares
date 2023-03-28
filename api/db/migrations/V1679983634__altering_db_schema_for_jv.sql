ALTER TABLE journal_vouchers
ADD COLUMN led_amount decimal(13,4);

ALTER TABLE journal_vouchers
ADD COLUMN sign_flag int2;

ALTER TABLE gl_codes
ADD COLUMN jv_category VARCHAR;

CREATE TABLE public.journal_voucher_categories (
id BIGSERIAL CONSTRAINT journal_voucher_category_PK PRIMARY KEY,
category VARCHAR,
description VARCHAR,
default_journal_code VARCHAR,
entity_code INT,
country_code VARCHAR,
valid_start DATE,
valid_end DATE,
created_at TIMESTAMP not null default now(),
updated_at timestamp not null default now(),
created_by UUID,
updated_by UUID
);

CREATE Table public.journal_voucher_codes (
id BIGSERIAL CONSTRAINT journal_code_PK PRIMARY KEY,
number VARCHAR NOT NULL,
description VARCHAR,
jv_category_id BIGINT NOT NULL REFERENCES journal_voucher_categories (id) ON DELETE CASCADE,
entity_code INT,
country_code VARCHAR,
created_at TIMESTAMP not null default now(),
updated_at TIMESTAMP not null default now(),
created_by UUID,
updated_by UUID
);

ALTER TABLE parent_journal_vouchers
ADD COLUMN  jv_code_num VARCHAR NOT NULL REFERENCES journal_voucher_codes (number) ON DELETE CASCADE;