ALTER TABLE public.parent_journal_vouchers ALTER COLUMN category TYPE varchar USING category::varchar;