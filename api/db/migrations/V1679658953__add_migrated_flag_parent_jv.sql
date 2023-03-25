ALTER TABLE parent_journal_vouchers
ADD COLUMN IF NOT EXISTS migrated BOOLEAN;

ALTER TYPE account_mode ADD VALUE 'OTHER';