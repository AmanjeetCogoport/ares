CREATE TYPE CYCLE_TYPE AS ENUM ('SOA', 'WIS', 'BALANCE_CONFIRMATION');
CREATE TYPE TRIGGER_TYPE AS ENUM ('ONE_TIME', 'PERIODIC');
CREATE TYPE FREQUENCY AS ENUM ('ONE_TIME', 'DAILY', 'MONTHLY', 'WEEKLY', 'BI_WEEKLY');
CREATE TYPE CYCLE_EXECUTION_STATUS AS ENUM ('SCHEDULED','CANCELLED','COMPLETED','IN_PROGRESS','FAILED');
CREATE TYPE CATEGORY AS ENUM ('CYCLE','MANUAL');

CREATE CAST (varchar AS CYCLE_TYPE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS TRIGGER_TYPE) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS FREQUENCY) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS CYCLE_EXECUTION_STATUS) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS CATEGORY) WITH INOUT AS IMPLICIT;

CREATE TABLE dunning_cycles (
id 				BIGSERIAL 		NOT NULL PRIMARY KEY,
name 			VARCHAR(500) 	NOT NULL UNIQUE,
cycle_type 		CYCLE_TYPE 		NOT NULL,
trigger_type 	TRIGGER_TYPE 	NOT NULL,
frequency   	FREQUENCY 	NOT NULL,
severity_level 	INTEGER 		NOT NULL DEFAULT 1,
filters 		JSONB,
schedule_rule 	JSONB,
template_id 	UUID 			NOT NULL,
category 		CATEGORY 		NOT NULL DEFAULT 'CYCLE',
is_active 		BOOLEAN 		NOT NULL DEFAULT TRUE,
deleted_at 		TIMESTAMP 		DEFAULT NULL,
created_by 		UUID 			NOT NULL,
updated_by 		UUID 			NOT NULL,
created_at 		TIMESTAMP   	DEFAULT CURRENT_TIMESTAMP,
updated_at 		TIMESTAMP   	DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE dunning_cycle_executions (
id 					BIGSERIAL 				NOT NULL PRIMARY KEY,
dunning_cycle_id  	BIGINT 					NOT NULL REFERENCES dunning_cycles (id) ON DELETE CASCADE,
template_id 		UUID 					NOT NULL,
status 				CYCLE_EXECUTION_STATUS 	NOT NULL DEFAULT 'SCHEDULED',
filters 			JSONB,
schedule_rule 		JSONB,
frequency    		FREQUENCY 			NOT NULL,
scheduled_at 		TIMESTAMP 				NOT NULL,
trigger_type 		TRIGGER_TYPE 			NOT NULL,
deleted_at 			TIMESTAMP 				DEFAULT NULL,
created_by 			UUID 					NOT NULL,
updated_by 			UUID 					NOT NULL,
created_at 			TIMESTAMP 				DEFAULT CURRENT_TIMESTAMP,
updated_at 			TIMESTAMP 				DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE dunning_master_exceptions (
id  					BIGSERIAL 			NOT NULL PRIMARY KEY,
trade_party_detail_id 	UUID 				NOT NULL UNIQUE,
trade_party_name        VARCHAR(100)        NOT NULL,
organization_id 		UUID,
registration_number 	VARCHAR(50) 		NOT NULL UNIQUE,
organization_segment 	ORGANIZATION_SEGMENT,
credit_days 			INTEGER NOT NULL DEFAULT 0,
credit_amount 			DECIMAL NOT NULL DEFAULT 0,
is_active 				BOOLEAN 			NOT NULL DEFAULT TRUE,
deleted_at 				TIMESTAMP 			DEFAULT NULL,
created_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
created_by 				UUID 				NOT NULL,
updated_by 				UUID 				NOT NULL
);


CREATE TABLE dunning_cycle_exceptions (
id   					BIGSERIAL 	NOT NULL PRIMARY KEY,
dunning_cycle_id 				BIGINT 		NOT NULL REFERENCES dunning_cycles (id) ON DELETE CASCADE,
trade_party_detail_id 	UUID  		NOT NULL,
registration_number		VARCHAR(50) NOT NULL,
deleted_at 				TIMESTAMP 	DEFAULT NULL,
created_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP,
created_by 				UUID 		NOT NULL,
updated_by 				UUID 		NOT NULL
);


CREATE TABLE dunning_email_audits (
id  					BIGSERIAL 	NOT NULL PRIMARY KEY,
execution_id 			BIGINT 		NOT NULL REFERENCES dunning_cycle_executions (id) ON DELETE CASCADE,
communication_id 		UUID,
trade_party_detail_id 	UUID 		NOT NULL,
is_success              BOOLEAN     NOT NULL,
error_reason            VARCHAR(100),
created_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP
);