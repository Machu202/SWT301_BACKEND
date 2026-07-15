# Security and database migration

1. Rotate the database password, Cloudinary key/secret, and JWT secret immediately if the old repository was ever shared.
2. Copy `.env.example` to `.env` and insert only the newly rotated values. `.env` is ignored by Git.
3. Run `DATABASE_MIGRATION_SECURITY_PERFORMANCE.sql` against PostgreSQL after backing up the database.
4. The duplicate preflight queries at the top of the script must return no rows. Resolve duplicates before continuing.
5. Start the backend with the required environment variables. It intentionally fails during startup when `JWT_SECRET` or payment-bank configuration is missing.

New orders store an immutable delivery snapshot. Existing orders are backfilled from their current Address row by the migration. Product and payment statuses are constrained to the application enums.
