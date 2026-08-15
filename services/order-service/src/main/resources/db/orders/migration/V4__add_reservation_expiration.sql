ALTER TABLE orders ADD COLUMN reservation_expires_at TIMESTAMPTZ;

CREATE INDEX idx_orders_awaiting_payment_expiry
    ON orders (reservation_expires_at)
    WHERE status = 'AWAITING_PAYMENT' AND reservation_expires_at IS NOT NULL;
