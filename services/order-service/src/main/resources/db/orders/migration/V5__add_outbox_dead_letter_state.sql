ALTER TABLE order_outbox ADD COLUMN dead_lettered_at TIMESTAMPTZ;
CREATE INDEX idx_order_outbox_dead_lettered ON order_outbox (dead_lettered_at) WHERE dead_lettered_at IS NOT NULL;
