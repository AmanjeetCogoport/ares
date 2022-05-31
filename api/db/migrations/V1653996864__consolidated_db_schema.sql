CREATE TYPE public."account_mode" AS ENUM (
	'AR',
	'AP');

CREATE TYPE public."account_type" AS ENUM (
    	'SINV',
    	'PINV',
    	'SCN',
    	'SDN',
    	'PCN',
    	'PDN',
    	'REC',
    	'PAY');

CREATE TYPE public."document_status" AS ENUM (
	'FINAL',
	'CANCELLED',
	'PROFORMA');

CREATE TYPE public."payment_mode" AS ENUM (
	'DD',
	'CASH',
	'CHEQUE',
	'NET_BANKING',
	'UPI',
	'BANK');

CREATE TYPE public."service_type" AS ENUM (
    	'FCL_FREIGHT',
    	'LCL_FREIGHT',
    	'AIR_FREIGHT',
    	'FTL_FREIGHT',
    	'LTL_FREIGHT',
    	'HAULAGE_FREIGHT',
    	'FCL_CUSTOMS',
    	'AIR_CUSTOMS',
    	'LCL_CUSTOMS');

 CREATE CAST (varchar AS ACCOUNT_MODE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar AS PAYMENT_MODE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar AS ACCOUNT_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar as SERVICE_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar as DOCUMENT_STATUS) WITH INOUT AS IMPLICIT;


 CREATE TABLE public.account_master (
 	acc_code serial4 NOT NULL,
 	acc_short_desc varchar(10) NOT NULL,
 	acc_long_desc varchar(100) NULL,
 	created_at timestamp NOT NULL DEFAULT now(),
 	updated_at timestamp NOT NULL DEFAULT now(),
 	is_active bool NOT NULL DEFAULT true,
 	is_deleted bool NOT NULL DEFAULT false,
 	CONSTRAINT acc_master_code_pk PRIMARY KEY (acc_code)
 );

CREATE TABLE public.account_country_mapping (
	acc_code int4 NOT NULL,
	country_code varchar(10) NOT NULL,
	created_at timestamp NOT NULL DEFAULT now(),
	updated_at timestamp NOT NULL DEFAULT now(),
	is_active bool NOT NULL DEFAULT true,
	CONSTRAINT account_country_mapping_pkey PRIMARY KEY (acc_code, country_code)
);


CREATE TABLE public.payments (
	id bigserial NOT NULL,
	entity_code int2 NOT NULL,
	org_serial_id int4 NULL,
	sage_organization_id varchar(20) NULL,
	organization_id uuid NULL,
	organization_name varchar(200) NULL,
	acc_code int4 NOT NULL,
	acc_mode public."account_mode" NOT NULL,
	sign_flag int2 NOT NULL,
	currency varchar(10) NOT NULL,
	amount numeric(13, 4) NOT NULL DEFAULT 0,
	led_currency varchar(10) NOT NULL,
	led_amount numeric(13, 4) NOT NULL DEFAULT 0,
	pay_mode public."payment_mode" NULL,
	narration varchar(200) NULL,
	cogo_account_no varchar(30) NULL,
	ref_account_no varchar(30) NULL,
	bank_name varchar(100) NULL,
	trans_ref_number varchar(50) NULL,
	ref_payment_id int8 NULL,
	transaction_date date NULL,
	is_posted bool NOT NULL DEFAULT false,
	is_deleted bool NOT NULL DEFAULT false,
	created_at timestamp NOT NULL DEFAULT now(),
	updated_at timestamp NOT NULL DEFAULT now(),
	CONSTRAINT payments_pk PRIMARY KEY (id)
);



CREATE TABLE public.account_utilizations (
	id bigserial NOT NULL,
	document_no int8 NOT NULL,
	document_value varchar(25) NULL,
	zone_code varchar(10) NOT NULL,
	"service_type" public."service_type" NULL,
	"document_status" public."document_status" NOT NULL,
	entity_code int4 NOT NULL,
	category varchar(20) NULL,
	org_serial_id int8 NOT NULL,
	sage_organization_id varchar(20) NULL,
	organization_id uuid NULL,
	organization_name varchar(200) NULL,
	acc_code int4 NOT NULL,
	acc_type public."account_type" NOT NULL,
	acc_mode public."account_mode" NOT NULL,
	sign_flag int2 NOT NULL,
	currency varchar(10) NOT NULL,
	led_currency varchar(10) NOT NULL,
	amount_curr numeric(13, 4) NOT NULL,
	amount_loc numeric(13, 4) NOT NULL,
	pay_curr numeric(13, 4) NOT NULL DEFAULT 0,
	pay_loc numeric(13, 4) NOT NULL DEFAULT 0,
	due_date date NULL,
	transaction_date date NULL,
	created_at timestamp NOT NULL DEFAULT now(),
	updated_at timestamp NOT NULL DEFAULT now(),
	CONSTRAINT acc_utils_pk PRIMARY KEY (id),
	CONSTRAINT account_utilizations_category_check CHECK (((category)::text = ANY ((ARRAY['ASSET'::character varying, 'NON_ASSET'::character varying])::text[]))),
	CONSTRAINT account_utilizations_zone_code_check CHECK (((zone_code)::text = ANY ((ARRAY['NORTH'::character varying, 'SOUTH'::character varying, 'EAST'::character varying, 'WEST'::character varying])::text[])))
);


-- public.account_utilizations foreign keys
ALTER TABLE public.account_utilizations ADD CONSTRAINT account_utilizations_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES public.account_master(acc_code);

-- public.payments foreign keys
ALTER TABLE public.payments ADD CONSTRAINT payments_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES public.account_master(acc_code);