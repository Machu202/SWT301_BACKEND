# SWT301 Backend Automated Test Suite

This backend includes isolated automated tests for authentication, JWT handling, addresses, products, cart, vouchers, profiles, checkout, orders, payment verification, receipt handling, validation, exception mapping, controllers, security helpers and endpoint contracts.

## Safety model

- `mvn test` uses Mockito and local test objects.
- It does not start the production application context.
- It does not connect to the configured PostgreSQL database.
- It does not upload to or delete from the configured Cloudinary account.
- It does not create, update or delete real orders, products, addresses, carts, vouchers or payments.

The project configuration and production code are compiled as part of the test run. The exact `application.yml` values and exact configured MB Bank QR image are therefore included in the build, but external services are not contacted by the automated unit suite.

## Run on Windows

```powershell
.\scripts\run-backend-tests.ps1 -OpenCoverage
```

or:

```powershell
.\mvnw.cmd clean test
```

## Verified result for this package

```text
Tests run: 125
Failures: 0
Errors: 0
Skipped: 0
Build: SUCCESS
```

JaCoCo coverage from the verified run:

- Line coverage: 92.94%
- Instruction coverage: 88.74%
- Method coverage: 82.46%
- Class coverage: 92.45%
- Branch coverage: 67.41%

## Reports

After a test run:

- Test results: `target/surefire-reports`
- Coverage report: `target/site/jacoco/index.html`
- Machine-readable coverage: `target/site/jacoco/jacoco.xml`

## Covered workflows

- Username and Vietnamese-phone login
- Strict registration role handling
- JWT configuration, generation, expiration and malformed-token behavior
- Address list/create/update/delete, ownership, phone normalization and default-address rules
- Product active filtering, paging, create/update, image replacement/removal and soft deletion
- Cart add/stack/update/remove/clear, stock bounds, current prices and inactive-product rejection
- Voucher validation and discount calculation
- Profile read/update and duplicate phone handling
- Checkout preview, current prices, stock, saved/manual address snapshots, COD, QR and idempotency
- Six-digit `ORD-XXXXXX` generation and collision retry
- Order item snapshots and paged Customer/Admin mapping
- State transitions, cancellation restoration and COD-delivery payment handling
- Receipt ownership checks before upload, receipt replacement cleanup and protected access
- Exact configured static payment QR resource loading
- Payment verification prerequisites and final states
- File type, MIME, extension, signature and size validation
- Exception-to-HTTP mappings
- DTO boundary validation
- All mapped controller endpoint contracts

## Live smoke test

A separate read-only smoke script is included:

```powershell
.\scripts\live-backend-smoke.ps1 `
  -CustomerUsername customer `
  -CustomerPassword 'Customer@123' `
  -AdminUsername admin `
  -AdminPassword 'Admin@123'
```

This script requires a running backend and valid test accounts. Do not run mutation workflows against production data without a database backup.
