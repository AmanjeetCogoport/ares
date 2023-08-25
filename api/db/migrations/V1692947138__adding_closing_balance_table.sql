CREATE TABLE closing_balances (
    id SERIAL PRIMARY KEY,
    organization_id UUID,
    sage_organization_id VARCHAR(50),
    registration_number VARCHAR(50),
    closing_balance_debit NUMERIC(18, 4),
    closing_balance_credit NUMERIC(18, 4),
    created_at DATE NOT NULL DEFAULT CURRENT_DATE
);