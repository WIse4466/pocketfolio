-- V1: Baseline init schema (rebased)

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Categories (tree structure)
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_parent_category FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE UNIQUE INDEX idx_categories_name_parent_id ON categories(name, parent_id);

-- Accounts (aligned with current JPA entity)
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    initial_balance DECIMAL(18, 2) NOT NULL,
    current_balance DECIMAL(18, 2) NOT NULL,
    include_in_net_worth BOOLEAN NOT NULL DEFAULT TRUE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    closing_day INTEGER,
    due_day INTEGER,
    autopay_account_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_autopay_account FOREIGN KEY (autopay_account_id) REFERENCES accounts (id),
    CONSTRAINT chk_account_type CHECK (type IN ('CASH', 'BANK', 'CREDIT_CARD', 'E_WALLET', 'INVESTMENT'))
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_autopay ON accounts(autopay_account_id);

