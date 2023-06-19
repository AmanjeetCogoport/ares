CREATE TYPE public."organization_segment" AS ENUM (
	'CHANNEL_PARTNER',
	'LONG_TAIL',
	'MID_SIZE',
	'ENTERPRISE'
	);

 CREATE CAST (varchar AS ORGANIZATION_SEGMENT) WITH INOUT AS IMPLICIT;

 CREATE TABLE public.organization_stakeholders (
 	id                                bigserial               NOT NULL,
 	organization_stakeholder_name     VARCHAR(200)            NOT NULL,
 	organization_stakeholder_id       UUID                    NOT NULL,
 	organization_id                   UUID                    NOT NULL   UNIQUE,
 	organization_segment              ORGANIZATION_SEGMENT    NOT NULL,
    created_by                        UUID                    NOT NULL,
    updated_by                        UUID                    NOT NULL,
 	created_at                        TIMESTAMP               NOT NULL   DEFAULT now(),
 	updated_at                        TIMESTAMP               NOT NULL   DEFAULT now()
 );
