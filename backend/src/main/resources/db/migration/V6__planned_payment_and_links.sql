-- V6: add planned/posted payment links and transaction status

ALTER TABLE transactions
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'POSTED',
  ADD COLUMN statement_id UUID;

ALTER TABLE transactions
  ADD CONSTRAINT chk_tx_status CHECK (status IN ('PENDING','POSTED'));

ALTER TABLE transactions
  ADD CONSTRAINT fk_tx_statement FOREIGN KEY (statement_id) REFERENCES statements(id);

CREATE INDEX idx_transactions_statement ON transactions(statement_id);

ALTER TABLE statements
  ADD COLUMN planned_tx_id UUID,
  ADD COLUMN paid_tx_id UUID;

ALTER TABLE statements
  ADD CONSTRAINT fk_stmt_planned_tx FOREIGN KEY (planned_tx_id) REFERENCES transactions(id);

ALTER TABLE statements
  ADD CONSTRAINT fk_stmt_paid_tx FOREIGN KEY (paid_tx_id) REFERENCES transactions(id);

