CREATE TYPE public."organization_segment" AS ENUM (
	'CHANNEL_PARTNER',
	'LONG_TAIL',
	'MID_SIZE',
	'ENTERPRISE'
	);
CREATE TYPE PUBLIC."stakeholder_type" AS ENUM ('CREDIT_CONTROLLER');
CREATE TYPE CYCLE_TYPE AS ENUM ('SOA', 'WIS', 'BALANCE_CONFIRMATION');
CREATE TYPE TRIGGER_TYPE AS ENUM ('ONE_TIME', 'PERIODIC');
CREATE TYPE FREQUENCY AS ENUM ('ONE_TIME', 'DAILY', 'MONTHLY', 'WEEKLY');
CREATE TYPE CYCLE_EXECUTION_STATUS AS ENUM ('SCHEDULED','CANCELLED','COMPLETED','IN_PROGRESS','FAILED');
CREATE TYPE CATEGORY AS ENUM ('CYCLE','MANUAL');
CREATE TYPE OBJECT_TYPE AS ENUM ('DUNNING');
CREATE TYPE TOKEN_TYPE AS ENUM ('RELEVANT_USER', 'DUNNING_PAYMENT');


 CREATE CAST (varchar AS ORGANIZATION_SEGMENT) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS STAKEHOLDER_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS CYCLE_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS TRIGGER_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS FREQUENCY) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS CYCLE_EXECUTION_STATUS) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS CATEGORY) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS OBJECT_TYPE) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS TOKEN_TYPE) WITH INOUT AS IMPLICIT;


 CREATE TABLE public.organization_stakeholders (
 	id                                BIGSERIAL               NOT NULL,
 	organization_stakeholder_name     VARCHAR(200)            NOT NULL,
 	organization_stakeholder_id       UUID                    NOT NULL,
 	organization_stakeholder_type     STAKEHOLDER_TYPE        NOT NULL,
 	organization_id                   UUID                    NOT NULL,
 	organization_segment              ORGANIZATION_SEGMENT    NOT NULL,
    created_by                        UUID                    NOT NULL,
    updated_by                        UUID                    NOT NULL,
    is_active                         BOOLEAN 		          NOT NULL   DEFAULT TRUE,
 	created_at                        TIMESTAMP               NOT NULL   DEFAULT CURRENT_TIMESTAMP,
 	updated_at                        TIMESTAMP               NOT NULL   DEFAULT CURRENT_TIMESTAMP,
 	CONSTRAINT org_stakeholder_uniqueness UNIQUE (organization_id, organization_stakeholder_type)
 );

CREATE TABLE dunning_cycles (
id 				BIGSERIAL 		NOT NULL PRIMARY KEY,
name 			VARCHAR(300) 	NOT NULL UNIQUE,
cycle_type 		CYCLE_TYPE 		NOT NULL,
trigger_type 	TRIGGER_TYPE 	NOT NULL,
entity_code     INTEGER         NOT NULL,
frequency   	FREQUENCY 	    NOT NULL,
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
entity_code         INTEGER                 NOT NULL,
filters 			JSONB,
schedule_rule 		JSONB,
frequency    		FREQUENCY 			NOT NULL,
scheduled_at 		TIMESTAMP 				NOT NULL,
trigger_type 		TRIGGER_TYPE 			NOT NULL,
service_id          UUID,
deleted_at 			TIMESTAMP 				DEFAULT NULL,
created_by 			UUID 					NOT NULL,
updated_by 			UUID 					NOT NULL,
created_at 			TIMESTAMP 				DEFAULT CURRENT_TIMESTAMP,
updated_at 			TIMESTAMP 				DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE dunning_master_exceptions (
id  					BIGSERIAL 			NOT NULL PRIMARY KEY,
trade_party_detail_id 	UUID 				NOT NULL,
trade_party_name        VARCHAR(500)        NOT NULL,
organization_id 		UUID,
registration_number 	VARCHAR(50) 		NOT NULL,
organization_segment 	ORGANIZATION_SEGMENT,
is_active 				BOOLEAN 			NOT NULL DEFAULT TRUE,
entity_code             INTEGER             NOT NULL,
deleted_at 				TIMESTAMP 			DEFAULT NULL,
created_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP 			DEFAULT CURRENT_TIMESTAMP,
created_by 				UUID 				NOT NULL,
updated_by 				UUID 				NOT NULL,
CONSTRAINT registration_number_deleted_at_unique UNIQUE (registration_number, deleted_at)
);


CREATE TABLE dunning_cycle_exceptions (
id   					BIGSERIAL 	NOT NULL PRIMARY KEY,
dunning_cycle_id 	     BIGINT 		NOT NULL REFERENCES dunning_cycles (id) ON DELETE CASCADE,
trade_party_detail_id 	UUID  		NOT NULL,
registration_number		VARCHAR(50) NOT NULL,
deleted_at 				TIMESTAMP 	DEFAULT NULL,
created_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP,
created_by 				UUID 		NOT NULL,
updated_by 				UUID 		NOT NULL,
CONSTRAINT registration_number_dunning_cycle_id_deleted_at_unique UNIQUE (registration_number, dunning_cycle_id, deleted_at)
);


CREATE TABLE dunning_email_audits (
id  					BIGSERIAL 	NOT NULL PRIMARY KEY,
execution_id 			BIGINT 		NOT NULL REFERENCES dunning_cycle_executions (id) ON DELETE CASCADE,
communication_id 		UUID,
email_recipients        VARCHAR(50),
user_id                 UUID,
trade_party_detail_id 	UUID 		NOT NULL,
organization_id         UUID,
is_success              BOOLEAN     DEFAULT FALSE,
error_reason            TEXT,
created_at 				TIMESTAMP 	DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
id  					BIGSERIAL 		NOT NULL PRIMARY KEY,
object_id 				BIGINT 			NOT NULL,
object_type				OBJECT_TYPE		NOT NULL,
token_type				TOKEN_TYPE 	    NOT NULL,
token 					VARCHAR(100)	NOT NULL,
data 					JSONB,
expiry_time				TIMESTAMP,
created_at 				TIMESTAMP 		DEFAULT CURRENT_TIMESTAMP,
updated_at 				TIMESTAMP   	DEFAULT CURRENT_TIMESTAMP
);