ALTER TABLE orders
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ALTER COLUMN currency DROP DEFAULT;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'AWAITING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'REJECTED')),
    ADD CONSTRAINT chk_orders_total_amount
        CHECK (total_amount >= 0);

ALTER TABLE order_items
    ADD COLUMN line_number INT;

WITH numbered AS (
    SELECT id,
           row_number() OVER (PARTITION BY order_id ORDER BY id) - 1 AS line_number
      FROM order_items
)
UPDATE order_items item
   SET line_number = numbered.line_number
  FROM numbered
 WHERE item.id = numbered.id;

ALTER TABLE order_items
    ALTER COLUMN line_number SET NOT NULL,
    ADD CONSTRAINT uq_order_items_order_sku UNIQUE (order_id, sku),
    ADD CONSTRAINT uq_order_items_order_line UNIQUE (order_id, line_number);

DROP INDEX idx_orders_customer_id;
DROP INDEX idx_orders_status;

CREATE INDEX idx_orders_customer_created
    ON orders (customer_id, created_at DESC, id DESC);
CREATE INDEX idx_orders_status_updated
    ON orders (status, updated_at);
CREATE INDEX idx_orders_created
    ON orders (created_at DESC, id DESC);

CREATE TABLE order_idempotency (
    id              UUID PRIMARY KEY,
    customer_id     UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    order_id        UUID REFERENCES orders (id),
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_order_idempotency_customer_key
        UNIQUE (customer_id, idempotency_key),
    CONSTRAINT uq_order_idempotency_order
        UNIQUE (order_id)
);

CREATE INDEX idx_order_idempotency_created
    ON order_idempotency (created_at);
