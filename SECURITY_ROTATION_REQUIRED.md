# Required credential rotation

The previous source package contained live-looking database, Cloudinary, and JWT credentials. Moving them to environment variables prevents future commits, but does not revoke values that were already exposed.

Before running or publishing this backend:

1. Change the database password in the database provider and update `DB_PASSWORD`.
2. Revoke the previous Cloudinary API key/secret, create a new key, and update the Cloudinary variables.
3. Generate a new JWT secret and set `JWT_SECRET`. Existing tokens should become invalid.
4. Remove secrets from Git history if this repository was already pushed publicly.
5. Copy `.env.example` to `.env`; never commit `.env`.

The application now intentionally fails to start when required values are missing.
