-- Optional explicit PostgreSQL/Supabase migration.
-- Hibernate ddl-auto=update also sees the matching JPA unique constraints.
-- Run the duplicate checks first. Resolve any returned rows before creating indexes.

-- Duplicate diagnostics
SELECT email, COUNT(*) FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1;
SELECT phone, COUNT(*) FROM users WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;
SELECT cart_id, product_id, COUNT(*) FROM cart_items GROUP BY cart_id, product_id HAVING COUNT(*) > 1;

-- Constraints requested by the software test findings
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone ON users (phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_items_cart_product ON cart_items (cart_id, product_id);
