-- V4: add due fields to accounts
ALTER TABLE accounts
  ADD COLUMN due_month_offset SMALLINT NOT NULL DEFAULT 1,
  ADD COLUMN due_holiday_policy VARCHAR(16) NOT NULL DEFAULT 'NONE',
  ADD COLUMN autopay_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Value constraints
ALTER TABLE accounts
  ADD CONSTRAINT chk_due_month_offset CHECK (due_month_offset IN (0,1,2));

ALTER TABLE accounts
  ADD CONSTRAINT chk_due_holiday_policy CHECK (due_holiday_policy IN ('NONE','ADVANCE','POSTPONE'));

