-- V3: Create transactions table for MVP (income/expense/transfer)

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    kind VARCHAR(20) NOT NULL,
    amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),

    -- For income/expense
    account_id UUID,

    -- For transfer
    source_account_id UUID,
    target_account_id UUID,

    category_id UUID,
    notes TEXT,

    currency_code VARCHAR(3) NOT NULL,
    fx_rate_used DECIMAL(18,6),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_target FOREIGN KEY (target_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES categories(id),

    CONSTRAINT chk_tx_kind CHECK (kind IN ('INCOME','EXPENSE','TRANSFER')),
    -- Non-transfer: require account_id and no source/target
    CONSTRAINT chk_tx_income_expense CHECK (
        (kind IN ('INCOME','EXPENSE') AND account_id IS NOT NULL AND source_account_id IS NULL AND target_account_id IS NULL)
        OR kind = 'TRANSFER'
    ),
    -- Transfer: require both source/target and they must differ; account_id must be NULL
    CONSTRAINT chk_tx_transfer CHECK (
        (kind = 'TRANSFER' AND account_id IS NULL AND source_account_id IS NOT NULL AND target_account_id IS NOT NULL AND source_account_id <> target_account_id)
        OR kind IN ('INCOME','EXPENSE')
    )
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_occurred_at ON transactions(occurred_at);
CREATE INDEX idx_transactions_kind ON transactions(kind);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_source ON transactions(source_account_id);
CREATE INDEX idx_transactions_target ON transactions(target_account_id);

