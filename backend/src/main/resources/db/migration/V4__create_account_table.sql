CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- Storing enum name as string
    currency_code VARCHAR(3) NOT NULL,
    initial_balance DECIMAL(18, 2) NOT NULL,
    current_balance DECIMAL(18, 2) NOT NULL,
    include_in_net_worth BOOLEAN NOT NULL DEFAULT TRUE, -- Added DEFAULT
    archived BOOLEAN NOT NULL DEFAULT FALSE,           -- Added DEFAULT
    closing_day INTEGER, -- Nullable for non-credit card accounts
    due_day INTEGER,     -- Nullable for non-credit card accounts
    autopay_account_id UUID, -- Self-referencing FK, Nullable
    notes TEXT, -- Changed from VARCHAR(500) to TEXT
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_autopay_account
        FOREIGN KEY (autopay_account_id)
        REFERENCES accounts (id),

    CONSTRAINT chk_account_type -- Added CHECK constraint
        CHECK (type IN ('CASH', 'BANK', 'CREDIT_CARD', 'E_WALLET', 'INVESTMENT'))
);

-- Added Indexes
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_autopay ON accounts(autopay_account_id);