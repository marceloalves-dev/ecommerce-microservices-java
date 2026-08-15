ALTER TABLE payment_outbox ADD COLUMN dead_lettered_at TIMESTAMPTZ;
CREATE INDEX idx_payment_outbox_dead_lettered ON payment_outbox (dead_lettered_at) WHERE dead_lettered_at IS NOT NULL;
