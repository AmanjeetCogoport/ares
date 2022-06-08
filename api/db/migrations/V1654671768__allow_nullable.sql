ALTER TYPE service_type ADD VALUE 'NA';
ALTER TABLE account_utilizations
ALTER COLUMN zone_code drop NOT NULL;