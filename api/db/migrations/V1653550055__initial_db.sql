CREATE TYPE ACCOUNT_MODE AS ENUM ('AR', 'AP');
CREATE TYPE PAYMENT_MODE as ENUM('DD','CASH','CHEQUE','NET_BANKING','UPI','BANK');
CREATE TYPE ACCOUNT_TYPE as ENUM ('SINV','PINV','SCN','SDN','PCN','PDN','REC','PAY');
create TYPE SERVICE_TYPE as ENUM ('FCL_FREIGHT','LCL_FREIGHT','AIR_FREIGHT','FTL_FREIGHT','LTL_FREIGHT','HAULAGE_FREIGHT','FCL_CUSTOMS','AIR_CUSTOMS','LCL_CUSTOMS');
CREATE TYPE DOCUMENT_STATUS as ENUM ('FINAL','CANCELLED','PROFORMA');

CREATE CAST (varchar AS ACCOUNT_MODE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS PAYMENT_MODE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS ACCOUNT_TYPE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar as SERVICE_TYPE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar as DOCUMENT_STATUS) WITH INOUT AS IMPLICIT;

CREATE  TABLE account_master (
    acc_code serial NOT NULL,
    acc_short_desc varchar(10) NOT NULL,
    acc_long_desc varchar(100) NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    modified_at timestamp NOT NULL DEFAULT now(),
    is_active bool not NULL DEFAULT true,
    is_deleted bool not NULL DEFAULT false,
    CONSTRAINT acc_master_code_pk PRIMARY KEY (acc_code)
);


CREATE  TABLE account_country_mapping (
    acc_code int NOT NULL,
    country_code varchar(10) NOT NULL,
    created_at timestamp not NULL DEFAULT now(),
    modified_at timestamp not NULL DEFAULT now(),
    is_active bool not NULL DEFAULT true,
    CONSTRAINT account_country_mapping_pkey PRIMARY KEY (acc_code, country_code)
);

CREATE TABLE bank_master (
    id serial4 NOT NULL,
    bank_name varchar(100) NOT NULL,
    ifsc_code varchar(15) NULL,
    branch_name varchar(50) NULL,
    account_no varchar(30) NOT NULL,
    swift_code varchar(40) NULL,
    created_at timestamp NULL DEFAULT now(),
    modified_at timestamp NULL DEFAULT now(),
    is_active bool NULL DEFAULT true,
    is_deleted bool NULL DEFAULT false,
    CONSTRAINT bank_master_id_pk PRIMARY KEY (id)
);

CREATE  TABLE payment_files (
    id bigserial NOT NULL,
    file_name varchar(50) NOT NULL,
    file_url varchar(500) NOT NULL,
    bank_id int  NULL,
    uploaded_by varchar(50) NOT NULL,
    is_deleted bool NOT NULL DEFAULT false,
    is_posted bool NOT NULL DEFAULT false,
    created_at timestamp NULL DEFAULT now(),
    modified_at timestamp NULL DEFAULT now(),
    CONSTRAINT ar_pay_file_pk PRIMARY KEY (id)
);

ALTER TABLE payment_files ADD CONSTRAINT ar_payment_files_bank_id_fkey FOREIGN KEY (bank_id) REFERENCES bank_master(id);

create  table payments
(
 id bigserial,
 entity_code smallint not null,
 file_id bigint  null,
 org_serial_id int,
 sage_organization_id varchar(20),
 organization_id uuid,
 organization_name varchar(200),
 acc_code int  not null,
 acc_mode account_mode not null,
 sign_flag smallint not null,
 currency varchar(10) NOT NULL,
 amount numeric(13, 4) NOT NULL DEFAULT 0,
 led_currency varchar(10) NOT NULL,
 led_amount numeric(13, 4) NOT NULL DEFAULT 0,
 pay_mode payment_mode NULL,
 narration varchar(200) NULL,
 bank_id int null,
 trans_ref_number varchar(50) NULL,
 ref_payment_id BIGINT NULL,
 transaction_date date NULL,
 is_posted bool NOT NULL DEFAULT false,
 is_deleted bool NOT NULL DEFAULT false,
 created_at timestamp NULL DEFAULT now(),
 modified_at timestamp NULL DEFAULT now(),
 CONSTRAINT payments_pk PRIMARY KEY (id)
);

-- public.payments foreign keys
ALTER TABLE payments ADD CONSTRAINT payments_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES  account_master(acc_code);
ALTER table payments ADD CONSTRAINT payments_bank_id_fkey FOREIGN KEY (bank_id) REFERENCES  bank_master(id);

CREATE TABLE account_utilizations (
    id bigserial NOT NULL,
    document_no bigint  NOT NULL,
    document_value varchar (25) null,
    zone_code varchar(10) check(zone_code in ('NORTH','SOUTH','EAST','WEST')) not null,
    service_type SERVICE_TYPE null,
  doc_status DOCUMENT_STATUS not null,
    entity_code int NOT NULL,
  category varchar(20) check(category in('asset','non_asset')),
    org_serial_id bigint NOT NULL,
    sage_organization_id varchar(20) NULL,
    organization_id uuid NULL,
    organization_name varchar(200) NULL,
    acc_code int NOT NULL,
    acc_type account_type NOT NULL,
    acc_mode account_mode NOT NULL,
    sign_flag smallint NOT NULL,
    currency varchar(10) not null,
    led_currency varchar(10) not null,
    amount_curr decimal(13, 4) NOT NULL,
    amount_loc decimal(13, 4) NOT NULL,
    pay_curr decimal(13, 4) NOT NULL DEFAULT 0,
    pay_loc decimal(13, 4) NOT NULL DEFAULT 0,
    due_date date NULL,
    transaction_date date NULL,
    created_at timestamp NULL DEFAULT now(),
    modified_at timestamp NULL DEFAULT now(),
    CONSTRAINT acc_utils_pk PRIMARY KEY (id)
);

-- public.account_utlizations foreign keys
ALTER TABLE account_utilizations ADD CONSTRAINT account_utilizations_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES account_master(acc_code);