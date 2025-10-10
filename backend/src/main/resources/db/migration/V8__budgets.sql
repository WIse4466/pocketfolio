-- V8: budgets tables (monthly total and per-category limits)

CREATE TABLE budgets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  month DATE NOT NULL, -- use first day of month
  total_limit DECIMAL(18,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_budgets_user_month UNIQUE (user_id, month)
);

CREATE TABLE category_budgets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  category_id UUID NOT NULL,
  month DATE NOT NULL,
  limit_amount DECIMAL(18,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_cat_budgets UNIQUE (user_id, category_id, month),
  CONSTRAINT fk_cb_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

