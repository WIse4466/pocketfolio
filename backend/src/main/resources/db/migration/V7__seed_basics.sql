-- V7: Seed basic categories and sample accounts for quicker first-run
-- Note: This seed is minimal and idempotent via NOT EXISTS checks by name.

-- Categories
INSERT INTO categories (id, name, parent_id, created_at, updated_at)
SELECT '11111111-1111-1111-1111-111111111001', 'Food', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Food' AND parent_id IS NULL);

INSERT INTO categories (id, name, parent_id, created_at, updated_at)
SELECT '11111111-1111-1111-1111-111111111002', 'Transport', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Transport' AND parent_id IS NULL);

INSERT INTO categories (id, name, parent_id, created_at, updated_at)
SELECT '11111111-1111-1111-1111-111111111003', 'Salary', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Salary' AND parent_id IS NULL);

-- Accounts (for default single user)
-- Cash Wallet
INSERT INTO accounts (
  id, user_id, name, type, currency_code, initial_balance, current_balance,
  include_in_net_worth, archived, closing_day, due_day, autopay_account_id,
  notes, created_at, updated_at
)
SELECT
  '22222222-2222-2222-2222-222222222001',
  '00000000-0000-0000-0000-000000000001',
  'Cash Wallet',
  'CASH',
  'TWD',
  0.00,
  0.00,
  TRUE,
  FALSE,
  NULL,
  NULL,
  NULL,
  NULL,
  NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name='Cash Wallet');

-- Bank Account
INSERT INTO accounts (
  id, user_id, name, type, currency_code, initial_balance, current_balance,
  include_in_net_worth, archived, closing_day, due_day, autopay_account_id,
  notes, created_at, updated_at
)
SELECT
  '22222222-2222-2222-2222-222222222002',
  '00000000-0000-0000-0000-000000000001',
  'My Bank',
  'BANK',
  'TWD',
  10000.00,
  10000.00,
  TRUE,
  FALSE,
  NULL,
  NULL,
  NULL,
  NULL,
  NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name='My Bank');

