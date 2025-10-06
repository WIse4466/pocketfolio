-- V5: credit card statements (MVP)

CREATE TABLE statements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    balance DECIMAL(18,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stmt_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_statements_account ON statements(account_id);
CREATE INDEX idx_statements_closing ON statements(closing_date);
CREATE INDEX idx_statements_due ON statements(due_date);
CREATE INDEX idx_statements_status ON statements(status);

