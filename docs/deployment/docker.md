# Docker local environment

Compose is a development environment, not a production deployment manifest. It runs one gateway, PostgreSQL 18, two user-service instances, one product-service and one order-service. Node services use the shared implementation with independent process identity. No Redis or monitoring container is added because those capabilities are deferred.

1. Stop native development: `scripts/stop-dev.ps1`.
2. Ensure Docker Desktop's Linux engine is running.
3. Copy `.env.example` to `.env`, replace the placeholder database password.
4. Run `docker compose config --quiet`, then `docker compose up --build -d`.
5. Inspect `docker compose ps` and `docker compose logs gateway`.
6. Open http://localhost:8080. After seeding, give the active health checks a few seconds to probe demo instances.

Only port 8080 is published, bound to 127.0.0.1. PostgreSQL and service ports stay inside the Compose network. PostgreSQL data is in `pgdata`, mounted at the PostgreSQL 18 parent data directory. Reset-mailbox delivery is in `gateway-data`. `docker compose down` preserves both volumes; adding `--volumes` would erase them and is not part of normal shutdown.

The gateway image is built using the retained Maven wrapper and JDK 25, then runs on a JRE as a non-root user. The frontend is embedded into the gateway JAR. Demo images run as the Node image's non-root user. Standard Compose services do not enable chaos controls.

## Environment variables

| Variable | Default / meaning |
|---|---|
| PORT | 8080 gateway port |
| DATABASE_URL | Local H2 file when absent; JDBC PostgreSQL URL in Compose |
| DATABASE_USER | `sa` locally, `sentinelx` in Compose |
| DATABASE_PASSWORD | External secret; required by Compose |
| DEMO_SEED | false by default; true only for explicit demo setup |
| DEMO_HOST | localhost for native development, docker for Compose seeding |
| UPSTREAM_HOSTS | Exact permitted upstream hostnames, comma separated |
| RESET_DIRECTORY | Blank normally; explicit local private mailbox path |
| NODE_ENV | Must equal development to enable demo fault controls |
| DEMO_CONTROL_TOKEN | Required secret header value for enabled fault controls |

The initial inspection found Docker installed with its Linux engine stopped. Docker Desktop was launched and the engine subsequently became available. See the validation record for actual build and runtime results; native PostgreSQL compatibility was also tested independently.
