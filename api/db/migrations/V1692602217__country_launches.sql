ALTER TABLE account_utilizations DROP CONSTRAINT account_utilizations_zone_code_check;
ALTER TABLE account_utilizations ADD CONSTRAINT account_utilizations_zone_code_check
CHECK (((zone_code)::text = ANY ((ARRAY['NORTH'::character varying, 'SOUTH'::character varying, 'EAST'::character varying, 'WEST'::character varying, 'VIETNAM'::character varying, 'SINGAPORE'::character varying, 'INDONESIA'::character varying, 'THAILAND'::character varying, 'CHINA'::character varying])::text[])));
