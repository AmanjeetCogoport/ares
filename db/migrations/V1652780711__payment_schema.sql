CREATE TYPE ACCOUNT_MODE AS ENUM ('ar', 'ap');
create type PAYMENT_MODE as ENUM('dd','cash','cheque','net_banking','upi','bank')
create type ACCOUNT_TYPE as ENUM ('sinv','pinv','scn','sdn','pcn','pdn','rec','pay')


create  table account_master
(
    acc_code SERIAL constraint acc_master_code_pk primary key,
    acc_short_desc  VARCHAR(10) NOT NULL,
    acc_long_desc   VARCHAR(100),
    created_at  TIMESTAMP NOT NULL  default NOW(),
    modified_at TIMESTAMP   default NOW(),
    is_active   BOOLEAN default true,
    is_deleted boolean default false
)

create table account_country_mapping
(
    acc_code    INT,
    country_code VARCHAR(10)    ,
    created_at  timestamp   default now(),
    modified_at timestamp   default now(),
    is_active   boolean default true,
    primary key(acc_code,country_code)
)


create table bank_master
(
  id SERIAL constraint bank_master_id_PK primary  key ,
  bank_name VARCHAR(100) NOT NULL,
  ifsc_code VARCHAR(15) null ,
  branch_name varchar(50),
  account_no varchar(30) not null,
  swift_code varchar (40),
  created_at timestamp default now(),
  modified_at timestamp default now(),
  is_active boolean default true,
  is_deleted boolean default false
)

create drop table acc_type_master
(
 id SERIAL constraint acc_type_master_id_PK primary key,
 type_code varchar(10) constraint type_code_UQ unique not null,
 description varchar (100),
 created_at timestamp default now(),
 modified_at timestamp default now()
)
insert into acc_type_master(type_code,description,sign_flag) values('SINV','sales invoice',1),
                                                         ('SCN','sales credit note',-1),('SDN','sales debit note',1),('PINV','purchase invoice',-1),
                                                         ('PCN','purchase credit note',1),('PDN','purchase debit note',1)

create table ar_payment_files
(
    id  bigserial constraint ar_pay_file_PK primary key,
    file_name   VARCHAR(50) NOT null,
    file_url    VARCHAR(500)    NOT null,
    bank_id INT references bank_master(id) null,
    uploaded_by VARCHAR(50) NOT null,
    is_deleted boolean not null default false,
    is_posted   BOOLEAN not null default false  ,
    created_at timestamp default now(),
    modified_at timestamp default now()
)
create  table payments
(
  id bigserial constraint payments_PK primary key,
  entity_code INT not null,
  entity_id UUID not null,
  file_id BIGINT,
  org_serial_id BIGINT not null ,
  sage_organization_id varchar (20),
  organization_id uuid not null,
  organization_name varchar(200),
  acc_code INT references account_master(acc_code) not null,
  acc_mode  ACCOUNT_MODE not null,
  sign_flag smallint not null,
  currency varchar (10) not null ,
  amount decimal(13,4) not null default 0,
  led_currency varchar (10) not null ,
  led_amount decimal(13,4) not null default 0,
  pay_mode PAYMENT_MODE,
  narration varchar (200),
  bank_id int references bank_master(id),
  trans_ref_number varchar(50),
  ref_payment_id bigint,
  transaction_date date,
  is_posted boolean not null default false,
  is_deleted boolean not null default false,
  created_at timestamp default now(),
  modified_at timestamp default now()
)

create  table account_utilizations
(
  id bigserial constraint acc_utils_PK primary key,
  document_no BIGINT not null ,
  entity_code INT not null,
  entity_id UUID not null,
  org_serial_id BIGINT not null ,
  sage_organization_id varchar (20),
  organization_id uuid not null,
  organization_name varchar(200),
  acc_code INT references account_master(acc_code) not null,
  acc_type ACCOUNT_TYPE not null,
  acc_mode  ACCOUNT_MODE not null,
  sign_flag smallint not null,
  amount_curr   DECIMAL(13,4)   NOT null,
  amount_loc    DECIMAL(13,4)   NOT null,
  pay_curr  DECIMAL(13,4)   NOT null default 0,
  pay_loc   DECIMAL(13,4)   NOT null default 0,
  due_date date ,
  transaction_date date,
  created_at timestamp default now(),
  modified_at timestamp default now()
)