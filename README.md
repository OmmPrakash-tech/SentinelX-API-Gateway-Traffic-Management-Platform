# SentinelX

**Distributed API Gateway & Traffic Management Platform**

A Spring Boot gateway with an operations console, developer portal, secure account management, persistent dynamic configuration, real traffic metrics, and independently running demo services.

## Start on this Windows machine

Open PowerShell 7 in `D:\projects\sentineIX`:

```powershell
.\scripts\start-dev.ps1
```

Open **http://localhost:8080**. The frontend is packaged and served by Spring Boot; no separate frontend server is necessary.

Development accounts all use the intentionally local password **`SentinelX-Local-2026!`**:

| Role | Email |
|---|---|
| Administrator | admin@sentinelx.local |
| Operator | operator@sentinelx.local |
| Developer | developer@sentinelx.local |
| User | user@sentinelx.local |

Accounts are seeded only when demo seeding is explicitly enabled, including the `dev` profile. These are not deployment credentials. Full details: [development credentials](docs/development-credentials.md).

The startup script selects installed JDK 25, builds the existing Maven project, and runs four Node demo processes plus the gateway in hidden windows. It records process IDs and start times so stopping targets only the processes it launched. Local PostgreSQL connection settings and reset-mailbox files live in `backend/data/`; database storage belongs to the installed PostgreSQL service; logs and process metadata are in `work/`.

```powershell
.\scripts\stop-dev.ps1                  # Retains data
.\scripts\start-dev.ps1 -SkipBuild       # Start a previously built package
.\scripts\run-tests.ps1                 # Java tests and JavaScript syntax checks
node tests/chaos/run.mjs                 # Running local stack required
.\scripts\run-load-test.ps1             # Measures traffic with configured limits
.\scripts\run-load-test.ps1 -Unthrottled # Temporarily disables and restores rate policies
```

To intentionally erase local demo data, stop the app and use `scripts/reset-dev.ps1 -ConfirmReset`. This backs up and clears only the configured `sentinelx` PostgreSQL schema. It preserves connection settings, older H2 files and Docker volumes, and does not stop the shared PostgreSQL service.

## Try the complete flow

1. Sign in as the administrator. Inspect Services, Instances, Routes and Rate limits. Health is measured over HTTP.
2. Sign out and sign in as the developer. Create an API key and copy the one-time secret.
3. Open API explorer, choose `/gateway/users`, and send a request. It works with your session or your API key.
4. Repeat to see alternating user-service instances, and inspect Requests, Usage and current quota.
5. Send requests to `/gateway/products` and `/gateway/orders` as well. POST a JSON object such as `{"name":"Sample"}` to create a demo record.
6. Run the failure tests to see healthy-instance selection, retry, timeout, circuit opening and recovery. They restore modified settings afterward.

The demo records start empty and are held in each demo process's memory. They demonstrate forwarding; they are not a shared application database. Gateway users, credentials, sessions, configuration, logs and audit events are persisted.

## What is implemented

- BCrypt passwords; registration, login/logout, password changes, reset links, session revocation, profile management.
- Backend-enforced ADMIN / OPERATOR / DEVELOPER / USER access and API-key ownership.
- Account activation, API-access control, role changes, forced logout and forced password reset.
- Hashed API keys with expiry, activation, revocation, rotation and a configurable rate tier.
- Persistent services, instance registration, dynamic longest-prefix routing and method allowlists.
- Real scheduled health checks, round robin and concurrent least-connections reservations.
- Process-local token buckets for global, IP, user, key and route scopes; 429 responses and quota headers.
- Per-instance/route CLOSED–OPEN–HALF_OPEN circuits; safe-read retries with exponential backoff.
- Separate connection, response-body and total request timeouts; request/response size limits.
- Request correlation, structured JSON logs, persisted request telemetry, audit events and real dashboards.
- PostgreSQL persistence and migrations, Docker Compose and Windows lifecycle scripts.

## Architecture and source map

```text
frontend/                  Browser ES modules, shared design system, two role-aware workspaces
backend/                   Existing Spring Boot 4.1.1 / Java 25 / Maven project, extended in place
  src/main/java/.../       Account, security, registry, gateway, traffic primitives and controllers
  src/main/resources/db/  Flyway relational schema migrations
  src/test/java/.../      HTTP integration, security, failure and concurrency tests
services/server.mjs        Shared demo-service implementation, independently instantiated
scripts/                   Windows startup, shutdown, test, load and reset commands
tests/chaos/               Live failure/recovery checks
tests/load/                Measured load runs and resource sampling
docs/                      Architecture, API, security, deployment and measured validation
```

[Architecture diagrams](docs/architecture/README.md) · [API reference](docs/api/README.md) · [Security](docs/security/README.md) · [Validation](docs/mvp-validation.md) · [Performance](docs/performance/README.md) · [AWS deployment design](docs/deployment/aws.md)

Latest verification: [PostgreSQL conversion and test results](docs/validation/postgresql-conversion.md).

## PostgreSQL and Docker

Local startup uses the fresh `sentinelx` database on PostgreSQL at `127.0.0.1:5432`, with the dedicated `sentinelx_app` account. The startup script loads its generated password from ignored `backend/data/postgresql.json`. Alternatively, set `DATABASE_URL`, `DATABASE_USER`, and `DATABASE_PASSWORD` together. Migrations run automatically. Keep the installed PostgreSQL Windows service running; application shutdown does not stop it. Older H2 records were not imported. H2 is now available only to isolated automated tests and is excluded from the application package.

```powershell
Copy-Item .env.example .env
# Edit .env and set a unique DATABASE_PASSWORD.
docker compose up --build -d
docker compose logs -f gateway
docker compose down
```

Stop the native stack first to free port 8080. Compose includes PostgreSQL, the gateway and four demo-service instances. Only the gateway is published, on loopback. Demo failure-control endpoints are disabled in the default containers. See [Docker instructions](docs/deployment/docker.md).

## Honest operating boundaries

This is a functional portfolio MVP, not a claim of production certification. The database is shared-capable, but token buckets, circuit state, health state and connection counts are **local to one gateway process**. Multiple independent demo services run behind it; globally coordinated multi-gateway policies require the deferred P1 shared-state work.

Production email delivery, Redis/distributed limits, caching, Prometheus/Grafana, tracing, WebSockets, weighted routing, canary releases and AWS deployment are not included. Log exploration is capped at the latest 1,000 rows; p95 and charts use the latest 10,000 retained requests. Request logs have a 30-day retention period. Connection failures during database outages are not buffered durably. See validation for actual tested outcomes and environment blockers.

No Git repository was initialized, no commits were created, and nothing was connected or pushed to GitHub.


