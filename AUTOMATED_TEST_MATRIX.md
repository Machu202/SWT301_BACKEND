# Automated Test Matrix

| Area | Automated coverage |
|---|---|
| Application | Main application entry-point contract |
| DTO validation | Registration, login, phone, cart quantity, product price/stock, checkout note/payment method and manual address |
| Authentication | Username/phone login, role restriction, registration, duplicate username/email/phone and password encoding |
| JWT/Security | Required configuration, token creation/read/expiry/malformed token, filter authentication, 401 JSON, user details and security beans |
| Addresses | List ordering, create, phone normalization, first/default rules, default replacement, ownership, delete conflict and default promotion |
| Products | Active-only public list, details, Admin/Public paging, create, replace/remove image and soft delete |
| Cart | Auto-create, add, stack, maximum stock, inactive product, update, current price, ownership, remove and clear |
| Vouchers | Missing, inactive, exhausted, expired, minimum subtotal and valid response |
| Profile | Read, missing user, update, phone normalization and duplicate phone |
| Checkout | Preview, empty cart, current price, active status, stock, shipping, voucher, COD, QR, idempotency and address modes |
| Orders | Six-digit code, item snapshots, paging, batched mapping, metrics, transition state machine, cancellation restore and COD delivered |
| QR/Receipts | Ownership preflight, payment state, exact configured QR resource, replacement cleanup and Customer/Admin protected receipt access |
| Payments | QR verification prerequisites, PAID/FAILED restriction and timestamps |
| File upload | MIME, extension, signature, size, upload/delete behavior and legacy download |
| Controllers | Direct delegation and response/header behavior |
| Endpoint contract | All mapped backend endpoints are present and method/path combinations are unique |
| Exceptions | 400, 401, 403, 404, 409, 413, 422, 500 and 502 mappings |
| CI | GitHub Actions runs `clean test` and uploads Surefire/JaCoCo reports |

## Verified totals

- 125 tests passed
- 0 failures
- 0 errors
- 0 skipped
- 92.94% line coverage
