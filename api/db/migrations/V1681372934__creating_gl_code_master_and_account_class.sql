CREATE TABLE public.gl_code_masters (
    id BIGSERIAL CONSTRAINT gl_code_master_PK PRIMARY KEY,
    account_code INT,
    description VARCHAR,
    led_account VARCHAR,
    account_type VARCHAR,
    class_code INT,
    account_class_id BIGINT NOT NULL REFERENCES account_classes (id) ON DELETE CASCADE,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP not null default now(),
    updated_at TIMESTAMP not null default now()
);

CREATE TABLE public.account_classes (
    id BIGSERIAL CONSTRAINT account_class_PK PRIMARY KEY,
    led_account VARCHAR,
    account_category VARCHAR,
    class_code INT,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP not null default now(),
    updated_at TIMESTAMP not null default now()
);