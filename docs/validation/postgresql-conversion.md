# Local PostgreSQL conversion — 15 September 2026

Local startup now uses PostgreSQL 18.3 at 127.0.0.1:5432, database `sentinelx`, dedicated application role `sentinelx_app`. A fresh database was created at the user's request; no records were imported from the previous H2 database. Other databases were not modified.

The startup helper loads the generated application credential from ignored `backend/data/postgresql.json`, or accepts the three DATABASE_* environment variables together. The PostgreSQL administrator password is not stored in project configuration. Keep the local settings private. Flyway applies the two existing schema migrations and the explicit development profile seeds four demo accounts.

The packaged runtime excludes H2. H2 remains a test-scope dependency for isolated in-memory tests. Tests also support TEST_DATABASE_URL, TEST_DATABASE_USER and TEST_DATABASE_PASSWORD for a separate PostgreSQL test database; never point tests at a database containing valued records.

Validation completed:

- Application package built successfully; no H2 driver in its nested dependencies.
- Both Flyway migrations applied successfully: 11 application tables plus migration history.
- All 19 Java tests passed with zero failures/errors against a separate fresh PostgreSQL database, which was removed after testing. JavaScript syntax checks passed.
- All 10 live smoke groups passed: frontend availability, sign-in, roles, healthy instances, API keys, load balancing, CRUD forwarding, metrics, rotation, revocation and logout. Evidence: [smoke results](postgresql-local-smoke.json).
- Stop/start with the existing package succeeded. Four demo accounts, 13 request records and two revoked API-key records persisted across the restart.
- Previous H2 file remained unchanged (34,598,912 bytes, last modified 14 September 2026).
- Application and all four demo processes stopped after validation; application ports are closed. The shared PostgreSQL service remains available.

Use `scripts/start-dev.ps1` to build and start, or add `-SkipBuild` to reuse the verified package. `scripts/stop-dev.ps1` stops only tracked application processes. It never stops the shared PostgreSQL service.

The reset script now requires explicit `-ConfirmReset`, backs up the configured dedicated database before clearing its public schema, and preserves credentials and older H2 files. Its syntax was checked; the destructive reset was not executed during conversion.

Earlier H2 load measurements and the 14 September validation report are historical results, not new PostgreSQL performance claims.
