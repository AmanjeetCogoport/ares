ALTER TABLE account_utilizations
ADD COLUMN is_proforma BOOLEAN DEFAULT FALSE;

ALTER TYPE account_type ADD VALUE 'JVTDS';
ALTER TYPE settlement_type ADD VALUE 'JVTDS';