CREATE TYPE public."organization_segment" AS ENUM (
	'CHANNEL_PARTNER',
	'LONG_TAIL',
	'MID_SIZE',
	'ENTERPRISE'
	);
CREATE TYPE PUBLIC."stakeholder_type" AS ENUM (
    'TRADE_FINANCE_AGENT',
    'ENTITY_MANAGER',
    'SUPPLY_AGENT',
    'SALES_AGENT',
    'BOOKING_AGENT',
    'PORTFOLIO_MANAGER',
    'CKAM',
    'CREDIT_CONTROLLER'
)

 CREATE CAST (varchar AS ORGANIZATION_SEGMENT) WITH INOUT AS IMPLICIT;
 CREATE CAST (VARCHAR AS STAKEHOLDER_TYPE) WITH INPUT AS IMPLICIT;

 CREATE TABLE public.organization_stakeholders (
 	id                                bigserial               NOT NULL,
 	organization_stakeholder_name     VARCHAR(200)            NOT NULL,
 	organization_stakeholder_id       UUID                    NOT NULL,
 	organization_stakeholder_type     STAKEHOLDER_TYPE        NOT NULL,
 	organization_id                   UUID                    NOT NULL,
 	organization_segment              ORGANIZATION_SEGMENT    NOT NULL,
    created_by                        UUID                    NOT NULL,
    updated_by                        UUID                    NOT NULL,
 	created_at                        TIMESTAMP               NOT NULL   DEFAULT now(),
 	updated_at                        TIMESTAMP               NOT NULL   DEFAULT now()
 );
