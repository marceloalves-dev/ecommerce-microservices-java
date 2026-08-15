ALTER TABLE orders ADD COLUMN reservation_id UUID;
CREATE UNIQUE INDEX uq_orders_reservation_id ON orders (reservation_id) WHERE reservation_id IS NOT NULL;

CREATE TABLE order_outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX idx_order_outbox_pending ON order_outbox (next_attempt_at, created_at) WHERE published_at IS NULL;

CREATE TABLE order_processed_events (
    consumer_name VARCHAR(150) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
);
