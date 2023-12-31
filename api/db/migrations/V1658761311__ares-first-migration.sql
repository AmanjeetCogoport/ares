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
	'PROFORMA',
	'DELETED');

CREATE TYPE public."payment_mode" AS ENUM (
	'IMPS',
	'CASH',
	'CHQ',
	'NEFT',
	'RTGS',
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
    	'LCL_CUSTOMS',
    	'NA',
    	'TRAILER_FREIGHT',
    	'STORE_ORDER',
    	'ADDITIONAL_CHARGE',
    	'FCL_CFS',
    	'ORIGIN_SERVICES',
    	'DESTINATION_SERVICES',
    	'FCL_CUSTOMS_FREIGHT',
    	'LCL_CUSTOMS_FREIGHT',
    	'AIR_CUSTOMS_FREIGHT');

 CREATE TYPE public."payment_code" AS ENUM (
	'PAY',
	'REC',
	'CTDS',
	'VTDS');

 CREATE CAST (varchar AS ACCOUNT_MODE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar AS PAYMENT_MODE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar AS ACCOUNT_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar as SERVICE_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar as DOCUMENT_STATUS) WITH INOUT AS IMPLICIT;
 CREATE CAST (varchar AS PAYMENT_CODE) WITH INOUT AS IMPLICIT;

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
	payment_code PAYMENT_CODE,
	payment_num bigint,
	payment_num_value varchar(25),
	exchange_rate decimal(9,4),
	bank_id uuid,
	CONSTRAINT payments_pk PRIMARY KEY (id)
);

CREATE TABLE public.account_utilizations (
	id bigserial NOT NULL,
	document_no int8 NOT NULL,
	document_value varchar(25) NULL,
	zone_code varchar(10)  NULL,
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
	taxable_amount numeric(13, 4) NOT null default 0,
	CONSTRAINT acc_utils_pk PRIMARY KEY (id),
	CONSTRAINT account_utilizations_category_check CHECK (((category)::text = ANY ((ARRAY['ASSET'::character varying, 'NON_ASSET'::character varying])::text[]))),
	CONSTRAINT account_utilizations_zone_code_check CHECK (((zone_code)::text = ANY ((ARRAY['NORTH'::character varying, 'SOUTH'::character varying, 'EAST'::character varying, 'WEST'::character varying])::text[])))
);


-- public.account_utilizations foreign keys
ALTER TABLE public.account_utilizations ADD CONSTRAINT account_utilizations_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES public.account_master(acc_code);

-- public.payments foreign keys
ALTER TABLE public.payments ADD CONSTRAINT payments_acc_code_fkey FOREIGN KEY (acc_code) REFERENCES public.account_master(acc_code);


create table public."payment_invoice_mapping"(
 id bigserial constraint pim_PK primary key,
 account_mode ACCOUNT_MODE,
 document_no bigint,
 payment_id bigint constraint payment_id_FK references payments(id),
 mapping_type varchar(10) check(mapping_type in('TDS','INVOICE','BILL')),
 currency varchar(5),
 sign_flag smallint,
 amount decimal(13,4) ,
 led_currency varchar(5),
 led_amount decimal(13,4),
 transaction_date date,
 created_at timestamp not null default now(),
 updated_at timestamp not null default now()
);

create table payment_sequence_numbers
(
  id serial,
  sequence_type varchar(10),
  next_sequence_number bigint,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now(),
  primary key(id)
);

insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('REC',202204,now(),now()),('PAY',202204,now(),now());


-- ENTRY IN ACCOUNT MASTER
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214028,'RD','Security Deposit-Machine');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214029,'RD','SecurityDeposit-Mumbai LodhaGH');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214030,'RD','Security Deposit-Mumbai S 7-8');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214031,'RD','Concor - ICD Tughlakabad');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240001,'AI','TDS on income 16-17');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240002,'AI','TDS on income 17-18');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240003,'AI','TDS on income 18-19');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240004,'AI','TDS on income 19-20');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240005,'AI','TDS on income 20-21');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620600,'SCIE','Salary - Contract');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225214,' ','AXIS FD - 920040061705992');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630700,' ','Reimbursement of Interest');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225500,' ','APAC FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620112,'SDIE','Employee Compensation Expense');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225184,' ','RBL FD - 709008697174');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225215,' ','AXIS FD - 920040061707226');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225216,' ','AXIS FD - 920040061707408');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225217,' ','AXIS FD - 920040061707570');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225218,' ','AXIS FD - 920040061707752');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225219,' ','AXIS FD - 920040062221523');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225220,' ','AXIS FD - 920040062216725');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225221,' ','AXIS FD - 920040062221194');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225222,' ','AXIS FD - 920040062221756');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214034,'RD','EMDs - Customer');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630800,' ','INTEREST EXPENSE _APAC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225185,' ','RBL FD - 709009075865');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225114,'TD','RBL FD - 709005874486');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214033,'RD','Freight Security Deposit');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (330001,' ','SBC Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (330002,' ','KKC Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (330003,' ','ST Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225115,'TD','RBL FD - 709005883952');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323013,'GSTL','CGST Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323014,'GSTL','SGST Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323015,'GSTL','IGST Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225209,' ','AXIS FD - 920040061705143');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225116,'TD','RBL FD - 709005905999');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225210,' ','AXIS FD - 920040061705318');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225211,' ','AXIS FD - 920040061705538');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225212,' ','AXIS FD - 920040061705707');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225213,' ','AXIS FD - 920040061705875');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225117,'TD','RBL FD - 709005938454');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225118,'TD','RBL FD - 709005946343');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225119,'TD','RBL FD - 709005951798');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225120,'TD','RBL FD - 709005970683');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225121,'TD','RBL FD - 709005993286');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225122,'TD','RBL FD - 709006012672');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225123,'TD','RBL FD - 709006020066');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225124,'TD','RBL FD - 709006043607');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225125,'TD','RBL FD - 709006060871');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225126,'TD','RBL FD - 709006093268');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225127,'TD','RBL FD - 709006095132');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225128,'TD','RBL FD - 709006100225');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225129,'TD','RBL FD - 709006146629');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225130,'TD','RBL FD - 709006171089');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225131,'TD','RBL FD - 709006171119');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225132,'TD','RBL FD - 709006174974');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225133,'TD','RBL FD - 709006188339');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225134,'TD','RBL FD - 709006200017');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225135,'TD','RBL FD - 709006220275');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225136,'TD','RBL FD - 709006233558');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225137,'TD','RBL FD - 709006242871');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225138,'TD','RBL FD - 709006253297');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225139,'TD','RBL FD - 709006272083');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225140,'TD','RBL FD - 709006272212');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225141,'TD','RBL FD - 709006276487');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225142,'TD','RBL FD - 709006292609');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225143,'TD','RBL FD - 709006292616');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225144,'TD','RBL FD - 709006316572');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225145,'TD','RBL FD - 709006323136');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225146,'TD','RBL FD - 709006379980');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225147,'TD','RBL FD - 709006399278');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225148,'TD','RBL FD - 709006417408');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225149,'TD','RBL FD - 709006444282');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225150,'TD','RBL FD - 709006455790');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225151,'TD','RBL FD - 709006480761');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225152,'TD','RBL FD - 709006493877');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225153,'TD','RBL FD - 709006506973');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225154,'TD','RBL FD - 709006586562');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225155,'TD','RBL FD - 709006586678');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225156,'TD','RBL FD - 709006586722');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225157,'TD','RBL FD - 709006590156');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225158,'TD','RBL FD - 709006604693');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225159,'TD','RBL FD - 709006809944');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225160,'TD','RBL FD - 709006810926');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225161,'TD','RBL FD - 709006843290');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225162,'TD','RBL FD - 709006958581');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225163,'TD','RBL FD - 709007025725');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225164,'TD','RBL FD - 709007025848');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225165,'TD','RBL FD - 709007082629');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225166,'TD','RBL FD - 709007722716');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225167,'TD','RBL FD - 709007860494');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225168,'TD','RBL FD - 709007867875');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225169,'TD','RBL FD - 709007867899');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225170,'TD','RBL FD - 709007973835');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225171,'TD','RBL FD - 709007973842');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225172,'TD','RBL FD - 709008104153');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225173,'TD','RBL FD - 709008587260');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225174,'TD','RBL FD - 709008616069');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225201,'TD','AXIS FD - 91904008819987');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225202,'TD','AXIS FD - 919040088215076');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225203,'TD','AXIS FD - 919040088215322');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225204,'TD','AXIS FD - 919040088215526');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225205,'TD','AXIS FD - 919040088215801');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225206,'TD','AXIS FD - 919040088216202');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225207,'TD','AXIS FD - 919040089639770');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225208,'TD','AXIS FD - 919040089639990');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225401,'TD','KOTAK FD  - 1412693798');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225402,'TD','KOTAK FD  - 1412693804');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225403,'TD','KOTAK FD  - 1412693811');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225404,'TD','KOTAK FD  - 1412693828');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225405,'TD','KOTAK FD  - 1412693835');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225406,'TD','KOTAK FD  - 1412693941');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225407,'TD','KOTAK FD  - 1412750538');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212600,'NCCEA','Leasehold Improvements ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212601,'NCCEA','Acc Depn Leasehold Improvement');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314007,'BODL','RBL WCDL Limit');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214001,'RD','Rent Deposit- Ahmedabad');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214002,'RD','Rent Deposit-Ahmedabad (GH)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214003,'RD','Rent Deposit-Ahmedabad(Office)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214004,'RD','Rent Deposit-Andheri(4th flr)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214015,'RD','Rent Deposit-Kolkata');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (226002,'AI','Accrued Revenue');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (117000,'FCE','Share Application A/c RBL');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214035,'AR',' Containers Security Deposit');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (870000,' ','Income Tax Refund');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214016,'RD','Rent Deposit-Goregaon Office');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (321001,'AI','Creditors employee');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222011,'BNK','YES BANK');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (571000,'TDE','Customs');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214017,'RD','Rent Deposit-Mumbai Guesthouse');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225179,'TD','RBL FD - 709008708788');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225180,'TD','RBL FD - 709008697174');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225181,'TD','RBL FD - 709009075865');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225182,'TD','RBL FD - 709009406188');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214018,'RD','Rent Deposit-Rajkot new office');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214019,'RD','Rent Deposit-Noida');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214020,'RD','Rent Deposit-Pinnacle Bonton ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214021,'RD','Rent Deposit-Pune');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214022,'RD','Rent Deposit-Rajkot Office');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214023,'RD','Security Deposit-ABL Noida');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214024,'RD','SecurityDeposit-Andheri7th flr');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214025,'RD','Security Deposit-Coffe Machine');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214026,'RD','Security Deposit-Jamshedpur');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214027,'RD','Security Deposit-Karnataka');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650800,'SAMIE','Redeemed Cogo point');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (433500,'AR','XXX XXX');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (114000,'FCE','Raghavendra - Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (115000,'FCE','Jaseem Poyil Yousuf - Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (116000,'FCE','Share Application Money');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (228000,'AI','Loans to Employees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (311003,'SLL','Purnendu Shekhar Loan A/C');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650501,'SAMIE','Discount On Purchase');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222008,'BNK','Citibank Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (850000,'INC','Bank interest income');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (860000,'INC','Other Income');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222009,'BNK','CC Avenue');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314006,'BODL','ICICI OD - 054405007720');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314005,'BODL','Kotak OD - 1412537955');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (224003,'PE','Other Prepaid Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (140000,'ICE','Retained Earnings');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (111000,'FCE','Purnendu Shekhar - Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (112000,'FCE','Kriti Dutta - Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212301,'NCOEA','Acc Depn Office Equipments');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212401,'NCCEA','Acc Depn Computer Equipment');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (113000,'FCE','Prashant Trivedi - Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (121000,'ICE','Accel India V (Mauritius)Ltd-E');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (122000,'ICE','Accel India V (Mauritius)-Pref');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (123000,'ICE','Gemini Equity Share Capital');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (124000,'ICE','Gemini Preference Share Capita');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (125000,'ICE','Internet Fund V PTE LTD-Equity');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (126000,'ICE','Internet Fund V PTE LTD-Pref. ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (130000,'ICE','Securities Premium Account');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (150000,'ICE','Other reserves');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (160000,'ICE','Non-Controlling Interest');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (211000,'NCIA','Intangible Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212000,'NCTA','Tangible Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212100,'NCFFA','Furniture and Fixtures');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212200,'NCPA','PPE');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212300,'NCOEA','Office Equipments');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212400,'NCCEA','Computer Equipment');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212500,'NCVA','Vehicle');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (213100,'INV','Cogoport Global B.V');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (213201,'MF','ICICI MUTUAL funds');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214000,'RD','Rent Deposit');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (221000,'CA','Cash');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222001,'BNK','RBL Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222002,'BNK','RBL EEFC - USD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222003,'BNK','ICICI Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222004,'BNK','DBS Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222005,'BNK','DBS EEFC -USD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222006,'BNK','Kotak Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222007,'BNK','Axis Current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (211001,'NCIA','Acc Depn Intangible Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (224001,'PE','Prepaid Expense For interest');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (224002,'PE','Prepaid Rent and Maintenance');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225100,'TD','RBL FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225200,'TD','AXIS FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225300,'TD','ICICI FD - 054410011199');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225400,'TD','Kotak FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (226001,'AI','Accrued Interest on FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (227000,'AI','Loans & Advances - current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (230000,'AI','Advance Tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (240000,'AI','TDS Receivable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (250000,'AI','Deferred Tax Asset');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (260000,'AI','Suspense A/c');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (310000,'NCL','Non-current liabilities');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (311001,'SLL','Term Loan Kotak_15');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (311002,'SLL','Term Loan Kotak _20');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (312001,'USL','Innovan Capital India Private ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (312002,'USL','Accel India V (Mauritius) Ltd.');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (312003,'USL','Gemini Investments,L.P.');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (312004,'USL','Internet Fund V PTE LTD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (312005,'LAL','Loans & Advances - non current');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (313000,'RBL','RBL Bill discounting');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212001,'NCTA','Acc Depn Tangible Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212101,'NCFFA','Acc Depn Furniture and Fixture');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212201,'NCPA','Acc Depn PPE');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314001,'BODL','Axis O/D A/c-919030087386886');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (212501,'NCVA','Acc Depn Vehicle');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (211100,'NCFA','Ficticious Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327100,'PVL','Audit Fees Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327200,'PVL','ESOP Stock Outstanding A/c');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314002,'BODL','RBL O/D A/c- 609000632974');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327300,'PVL','Provision for Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327400,'PVL','Provision For Interest');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327500,'PVL','Provision for Rent & Maintaina');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327600,'PVL','Other expesnes payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314003,'BODL','RBL O/D A/c- 609000715480');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (327700,'PVL','Rent Equilisation Account');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (411000,'FCR','FCL  Freight Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (412000,'FCR','FCL Freight Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (413000,'FCR','FCL Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (314004,'BODL','Kotak WCDL Limit');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (421000,'LCR','LCL Freight  Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (422000,'LCR','LCL Freight  Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (423000,'LCR','LCL Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (431000,'AIR','Air Freight  Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322001,'DTL','Employee Related');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (223000,'AR','Account Receivable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660100,'OFFIE','House Keeping Services');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (432000,'AIR','Air Freight  Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (433000,'AIR','Air Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322002,'DTL','Employees Contribution - LWF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322003,'DTL','Employees Contribution - PF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322004,'DTL','Employers Contribution - LWF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322005,'DTL','Employers Contribution - PF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322006,'DTL','Labour Welfare Fund');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322007,'DTL','PF Admin Charges Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322008,'DTL','Employees Contribution - ESIC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322010,'DTL','Profession Tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322011,'DTL','Provision for Gratuity');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322012,'DTL','Provision for Leave Encashment');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (322013,'DTL','Provision for income tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323001,'GSTL','Input CGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323002,'GSTL','Output CGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323003,'GSTL','Input SGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323004,'GSTL','Output SGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323005,'GSTL','Input IGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323006,'GSTL','Output IGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323007,'GSTL','Reverse Charge Input CGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323008,'GSTL','Reverse Charge Output CGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323009,'GSTL','Reverse Charge Input SGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323010,'GSTL','Reverse Charge Output SGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323011,'GSTL','Reverse Charge Input IGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (323012,'GSTL','Reverse Charge Output IGST');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (324001,'TSDL','TDS Payable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (325000,'AEL','Accrued Expense');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (326000,'AFCL','Advances from customers');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (441000,'FPR','Interest income from NBFC ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (442000,'FPR','Trade Finance');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (443000,'FPR','Insurance');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (451000,'SR','Subscription(SAAS)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (452000,'SR','Subscription(product)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (470000,'SR','Other Auxiliary Services');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (471000,'SR','Custom');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (472000,'SR','Stuffing');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (321000,'SC','Sundry Creditors');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (460000,'SR','Truck');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (511000,'FCDE','FCL  Freight Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (512000,'FCDE','FCL Freight  Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (513000,'FCDE','FCL Discount');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (514000,'FCDE','FCL Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (521000,'LCDE','LCL  Freight  Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (522000,'LCDE','LCL Freight  Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (523000,'LCDE','LCL Discount');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (524000,'LCDE','LCL Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (531000,'ADE','Air Freight Export');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (532000,'ADE','Air Freight Import');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (533000,'ADE','Air Discount');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (534000,'ADE','Air Locals');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (610100,'RIE','Facility Rent');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (610200,'RIE','Brokerage on Rent');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (610300,'RIE','Furniture Rent');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (610400,'RIE','Equipment on Rent');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620101,'SDIE','Directors Remuneration');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620102,'SDIE','Salary Perm');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620103,'SDIE','Employee Mediclaim');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620104,'SDIE','Employers Gratuity');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620105,'SDIE','Employers Leave Encashment');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620106,'SDIE','Employers LWF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620107,'SDIE','Employers PF');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620108,'SDIE','Bonus');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620109,'SDIE','Statutory Bonus');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620110,'SDIE','ESI');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620300,'SCIE','Stipend');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620400,'SCIE','PF Admin Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620500,'SCIE','Reimbursement Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630100,'IEIE','Interest on Bill Discounting_R');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630200,'IEIE','OD service Interest');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630300,'IEIE','Interest on WC LOAn');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630400,'IEIE','Interest on Term LOAn');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630500,'IEIE','Penalty on delayed statutory p');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (640100,'RMIE','Facility repairs and maintenan');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (640200,'RMIE','Equipment repairs and maintena');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (640300,'RMIE','Warranty Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650100,'SAMIE','Digital Marketing');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650200,'SAMIE','Advertisement Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650300,'SAMIE','Business Promotion and Sponsor');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650400,'SAMIE','Customer Support Service');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650500,'SAMIE','Discount On Sale');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650600,'SAMIE','Channel Partner Commission');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650700,'SAMIE','Campaign and Events');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660200,'OFFIE','Telephone & Internet Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660300,'OFFIE','Electricity');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660400,'OFFIE','Staff Welfare expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660500,'OFFIE','Donation');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660600,'OFFIE','Travelling Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660700,'OFFIE','Printing and stationery');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660800,'OFFIE','Lodging & Boarding Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660900,'OFFIE','Staff Insurance');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670100,'LFFIE','Audit Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670200,'LFFIE','Certification Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670300,'LFFIE','Consultancy Exp.');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670400,'LFFIE','Legal Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670500,'LFFIE','Legal Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670600,'LFFIE','ROC Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670700,'LFFIE','Advocate fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (670800,'LFFIE','Professional Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (680100,'BNCIE','Nostro Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (680200,'BNCIE','Loan Processing Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (680300,'BNCIE','Bank Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (690100,'HRIE','Bootcamp Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (690200,'HRIE','Relocation Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (690300,'HRIE','Recruitment Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (690400,'HRIE','Insurance');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611100,'SEIE','Cloud Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611200,'SEIE','Development Cost');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611300,'SEIE','Domain Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611400,'SEIE','Server Cost');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611500,'SEIE','Software Exp.');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611600,'SEIE','Techincal Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (611700,'SEIE','Technology License Fees');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (612100,'LFEIE','Realised Exchange Gain/Loss');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (612200,'LFEIE','Unrealised Exchange Gain/Loss');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (613200,'RTIE','Input SBC (RCM)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (613300,'RTIE','Input VAT');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (613400,'RTIE','Stamp Duty Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (613500,'RTIE','Deferred Tax Expense');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (710000,'DAIE','Amortisation');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (720000,'DAIE','Dep Expense Tangible Assets');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (730000,'DAIE','Dep Exp Furniture and Fixtures');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (740000,'DAIE','Dep Exp PPE');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (750000,'DAIE','Dep Exp Office Equipments');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (760000,'DAIE','Dep Exp Computer Equipment');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (770000,'DAIE','Dep Exp Vehicle');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (810000,'INC','Interest Income on FD');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (820000,'INC','Interest on Income Tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (830000,'INC','Profit on Sale of Asset');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (840000,'INC','Profit on Sale of Financial As');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (900000,'EI','Extraordinary items');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (999999,' ','Suspense Account');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (560000,'TDE','Truck Expense');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329000,'STPL','Output Service Tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329100,'STPL','Output SBC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329200,'STPL','Output KKC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328000,'STPL','Input Service Tax');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328100,'STPL','Input SBC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328200,'STPL','Input KKC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (512500,'FCDE','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (522500,'LCDE','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (532500,'ADE','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (565000,'TDE','Truck - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (534500,'ADE','Locals - Taxable ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (524500,'LCDE','Locals - Taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (514500,'FCDE','Locals - Taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (412500,'FCR','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (413500,'FCR','Locals - Taxable ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (422500,'LCR','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (423500,'LCR','Locals - Taxable ');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (432500,'AR','Freight - Import - Non-Taxab');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (900001,'DAIE','Round off');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (482000,'SR','Supplier Commission');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (492000,'SR','Nostro Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (465000,'SR','Truck taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (650900,'SAMIE','Marketing Expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (630600,'IEIE','Interest On Innovan Loan');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328300,'STPL','Reverse Charge Input Service T');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328400,'STPL','Reverse Charge Input SBC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (328500,'STPL','Reverse Charge Input KKC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225301,'TD','ICICI FD - 054413007781');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (211200,'NCFA','Capital Work In Progress');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660101,'OFFIE','Pre operative expenses');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225302,'TD','ICICI FD - 054413008417');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (414000,'FCR','Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (424000,'LCR','LCL Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (434000,'AIR','Air Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (515000,'FCDE','FCL Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (525000,'LCDE','LCL Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (535000,'ADE','Air Freight - Export - taxable');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (222010,'BNK','Razorpay transactions');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225101,'TD','RBL FD - 709005393246');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225102,'TD','RBL FD - 709005460382');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225103,'TD','RBL FD - 709005482070');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225104,'TD','RBL FD - 709005493021');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225105,'TD','RBL FD - 709005561713');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225106,'TD','RBL FD - 709005695777');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225107,'TD','RBL FD - 709005700488');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225108,'TD','RBL FD - 709005720035');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225109,'TD','RBL FD - 709005752234');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225110,'TD','RBL FD - 709005770726');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225111,'TD','RBL FD - 709005802908');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225112,'TD','RBL FD - 709005811191');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (780000,'DAIE','Depr on Leasehold Improvements');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214005,'RD','Rent Deposit-Andheri(6th flr)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214006,'RD','Rent Deposit-Andheri(7th flr)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214007,'RD','Rent Deposit-Andheri Office');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329300,'STPL','Reverse Charge Output Service');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329400,'STPL','Reverse Charge Output SBC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (329500,'STPL','Reverse Charge Output KKC');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (660102,'OFFIE','Debtors write off');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214008,'RD','Rent Deposit-Baroda');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214009,'RD','Rent Deposit-Chennai');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214010,'RD','Rent Deposit-Gandhidham');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214011,'RD','Rent Deposit-Gurgaon(cowork)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (592000,'TDE','Nostro Charges');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214012,'RD','Rent Deposit-Hyderabad');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214013,'RD','Rent Deposit-Indore(MP)');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214014,'RD','Rent Deposit-Jaipur Office');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225175,'TD','RBL FD - 709008778996');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225176,'TD','RBL FD - 709009123887');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225113,'TD','RBL FD - 709005838556');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225177,'TD','RBL FD - 709009406164');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (225178,'TD','RBL FD - 709010647237');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (620111,'SDIE','Basic Salary');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (214032,'RD','Security Deposit Write Off');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (321002,'SC','Reimbursement expense received');
insert into account_master(acc_code,acc_short_desc,acc_long_desc) values (223002,'AR','Reimbursement invoices raised');

--- ENTRY IN COUNTRY MAPPING FOR INDIA
insert into account_country_mapping(acc_code,country_code)
select acc_code,'IND' from account_master;
