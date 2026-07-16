# Backend startup repair

The previous package changed runtime configuration to environment placeholders but did not configure Spring Boot to load the included `.env` file. As a result, Hikari received the literal value `${DB_URL}`.

This package fixes that by:

- importing `.env` through `spring.config.import`,
- restoring the previous working database, JWT, and Cloudinary values inside the local `.env`,
- keeping `.env` excluded from Git,
- retaining the functional backend code changes from the 43-issue package.

Run from the backend root:

```powershell
.\mvnw.cmd spring-boot:run
```

Do not commit `.env` to a public repository. Rotate credentials if they have already been published.
