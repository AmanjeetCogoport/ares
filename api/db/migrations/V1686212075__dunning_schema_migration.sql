CREATE TYPE CYCLE_TYPE AS ENUM ('SOA', 'WIS', 'BALANCE_CONFIRMATION');
CREATE TYPE TRIGGER_TYPE AS ENUM ('ONE_TIME', 'PERIODIC');
CREATE TYPE SCHEDULE_TYPE AS ENUM ('ONE_TIME', 'DAILY', 'MONTHLY', 'WEEKLY', 'BI_WEEKLY');
CREATE TYPE CYCLE_EXECUTION_STATUS AS ENUM ('SCHEDULED','CANCELLED','COMPLETED');
CREATE TYPE ORG_SEGMENTATIONS AS ENUM ('LONG_TAIL','MID_SIZE','ENTERPRISE');
CREATE TYPE CATEGORY AS ENUM ('CYCLE','MANUAL');

CREATE TABLE dunning_cycles (
id 				BIGSERIAL 		NOT NULL PRIMARY KEY,
name 			VARCHAR(50) 	NOT NULL UNIQUE,
cycle_type 		CYCLE_TYPE 		NOT NULL,
trigger_type 	TRIGGER_TYPE 	NOT NULL,
schedule_type 	SCHEDULE_TYPE 	NOT NULL,
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
schedule_type 		SCHEDULE_TYPE 			NOT NULL,
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
organization_id 		UUID,
registration_no 		VARCHAR(50) 		NOT NULL UNIQUE,
Category 				ORG_SEGMENTATIONS,
credit_days 			INTEGER,
credit_amount 			BIGINT,
is_active 				BOOLEAN 			NOT NULL DEFAULT TRUE,
deleted_at 				TIMESTAMP 			DEFAULT NULL,
created_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
created_by 				UUID 				NOT NULL,
updated_by 				UUID 				NOT NULL
);


CREATE TABLE dunning_cycle_exceptions (
id   					BIGSERIAL 	NOT NULL PRIMARY KEY,
cycle_id 				BIGINT 		NOT NULL REFERENCES dunning_cycles (id) ON DELETE CASCADE,
trade_party_detail_id 	UUID  		NOT NULL,
organization_id 		UUID,
registration_no 		VARCHAR(50) NOT NULL,
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
trade_party_mapping_id 	UUID 		NOT NULL,
registration_no 		VARCHAR(50) NOT NULL,
created_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP
);