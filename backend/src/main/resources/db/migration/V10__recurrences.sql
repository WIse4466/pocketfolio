-- V10: monthly recurrences (INCOME/EXPENSE) with simple holiday policy and link to transactions

CREATE TABLE recurrences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  kind VARCHAR(20) NOT NULL CHECK (kind IN ('INCOME','EXPENSE')),
  amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
  currency_code VARCHAR(3) NOT NULL,
  account_id UUID NOT NULL,
  category_id UUID,
  day_of_month SMALLINT NOT NULL CHECK (day_of_month IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,31)),
  holiday_policy VARCHAR(16) NOT NULL DEFAULT 'NONE' CHECK (holiday_policy IN ('NONE','ADVANCE','POSTPONE')),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_rec_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_rec_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

ALTER TABLE transactions
  ADD COLUMN recurrence_id UUID;

ALTER TABLE transactions
  ADD CONSTRAINT fk_tx_recurrence FOREIGN KEY (recurrence_id) REFERENCES recurrences(id);

CREATE INDEX idx_transactions_recurrence ON transactions(recurrence_id);

