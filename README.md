# SWT301 E-commerce Backend

Spring Boot backend for the SWT301 testing frontend.

## Security setup required

The repository contains no live database password, JWT secret, Cloudinary secret, or bank-account values. Copy `.env.example` to `.env` and supply newly rotated values.

```powershell
Copy-Item .env.example .env
```

Required groups:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` (at least 32 random bytes) and `JWT_EXPIRATION_MS`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `PAYMENT_BANK_ID`, `PAYMENT_BANK_NAME`, `PAYMENT_ACCOUNT_NUMBER`, `PAYMENT_ACCOUNT_NAME`

The application intentionally refuses to start when required JWT or payment configuration is missing.

## Database migration

Back up PostgreSQL, then run:

```text
DATABASE_MIGRATION_SECURITY_PERFORMANCE.sql
```

The migration adds:

- case-insensitive unique email, unique phone, and unique cart-product constraints
- immutable delivery snapshots in `orders`
- product snapshots in `order_items`
- idempotency keys and unique order-code protection
- protected Cloudinary public IDs and payment timestamps
- Product/Payment status constraints
- indexes for paginated order and product access

Resolve any duplicate records reported by the preflight queries before creating the unique indexes.

## Run

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

## Important behavior

- Public catalog returns only ACTIVE products.
- Cart and checkout reject INACTIVE products.
- Checkout uses current Product prices and locks stock rows.
- Manual checkout addresses are stored as immutable Order snapshots, not standalone Address rows.
- Checkout requires `Idempotency-Key`.
- Products are deactivated rather than physically deleted.
- Receipts are stored as authenticated Cloudinary assets and served only through authorized endpoints.
- Customer/Admin order APIs provide pagination and order-item details.
- VietQR is generated per order using exact amount and order code; bank configuration comes from environment variables.
