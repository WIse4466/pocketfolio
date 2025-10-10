-- V9: FX rates (manual daily rates, base TWD)

CREATE TABLE fx_rates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  as_of_date DATE NOT NULL,
  base_ccy VARCHAR(3) NOT NULL,
  quote_ccy VARCHAR(3) NOT NULL,
  rate DECIMAL(18,6) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_fx UNIQUE(as_of_date, base_ccy, quote_ccy)
);

