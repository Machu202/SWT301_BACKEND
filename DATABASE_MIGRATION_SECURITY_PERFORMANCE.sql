-- SWT301 security/performance migration (PostgreSQL)
-- Back up the database before running. Resolve duplicate rows reported by the preflight queries first.

BEGIN;

-- 1) Preflight uniqueness checks. These SELECTs must return no rows before the indexes are created.
SELECT lower(email) AS duplicate_email, COUNT(*)
FROM users WHERE email IS NOT NULL AND btrim(email) <> ''
GROUP BY lower(email) HAVING COUNT(*) > 1;

SELECT phone AS duplicate_phone, COUNT(*)
FROM users WHERE phone IS NOT NULL AND btrim(phone) <> ''
GROUP BY phone HAVING COUNT(*) > 1;

SELECT cart_id, product_id, COUNT(*)
FROM cart_items GROUP BY cart_id, product_id HAVING COUNT(*) > 1;

-- 2) Columns required by immutable orders, idempotency, protected images, and history DTOs.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key varchar(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_name_snapshot varchar(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_phone_snapshot varchar(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS province_snapshot varchar(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS district_snapshot varchar(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ward_snapshot varchar(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS street_snapshot varchar(255);
-- Manual checkout stores only an immutable snapshot, so no Address row is required.
ALTER TABLE orders ALTER COLUMN address_id DROP NOT NULL;

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name_snapshot varchar(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_image_snapshot varchar(1000);

ALTER TABLE products ADD COLUMN IF NOT EXISTS image_public_id varchar(500);
ALTER TABLE products ADD COLUMN IF NOT EXISTS status varchar(20);
UPDATE products SET status = 'ACTIVE' WHERE status IS NULL OR btrim(status) = '';
ALTER TABLE products ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE products ALTER COLUMN status SET NOT NULL;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_public_id varchar(500);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_uploaded_at timestamp;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS verified_at timestamp;

-- 3) Backfill immutable snapshots for historical rows.
UPDATE orders o
SET receiver_name_snapshot = COALESCE(o.receiver_name_snapshot, a.receiver_name),
    receiver_phone_snapshot = COALESCE(o.receiver_phone_snapshot, a.receiver_phone),
    province_snapshot = COALESCE(o.province_snapshot, a.province),
    district_snapshot = COALESCE(o.district_snapshot, a.district),
    ward_snapshot = COALESCE(o.ward_snapshot, a.ward),
    street_snapshot = COALESCE(o.street_snapshot, a.street)
FROM addresses a
WHERE o.address_id = a.address_id;

UPDATE order_items oi
SET product_name_snapshot = COALESCE(oi.product_name_snapshot, p.product_name),
    product_image_snapshot = COALESCE(oi.product_image_snapshot, p.image)
FROM products p
WHERE oi.product_id = p.product_id;

-- Legacy public receipt rows get a timestamp so the UI can display historical timing when possible.
UPDATE payments
SET receipt_uploaded_at = COALESCE(receipt_uploaded_at, payment_date)
WHERE qr_image IS NOT NULL AND btrim(qr_image) <> '';

-- 4) Unique indexes. Email uniqueness is case-insensitive.
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_ci
    ON users (lower(email)) WHERE email IS NOT NULL AND btrim(email) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone
    ON users (phone) WHERE phone IS NOT NULL AND btrim(phone) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_items_cart_product
    ON cart_items (cart_id, product_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_order_code
    ON orders (order_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_user_idempotency
    ON orders (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 5) Query-supporting indexes for paginated history and batch mapping.
CREATE INDEX IF NOT EXISTS idx_orders_user_created_at
    ON orders (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_created_at
    ON orders (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_products_status_category
    ON products (status, category_id);

-- 6) Constrain free-form status columns after normalizing existing data.
UPDATE products SET status = upper(btrim(status));
UPDATE payments SET payment_status = upper(btrim(payment_status));

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_products_status') THEN
    ALTER TABLE products ADD CONSTRAINT ck_products_status
      CHECK (status IN ('ACTIVE', 'INACTIVE'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_payments_status') THEN
    ALTER TABLE payments ADD CONSTRAINT ck_payments_status
      CHECK (payment_status IN ('PENDING', 'AWAITING_PAYMENT', 'PENDING_VERIFICATION', 'PAID', 'FAILED', 'CANCELLED'));
  END IF;
END $$;

COMMIT;
