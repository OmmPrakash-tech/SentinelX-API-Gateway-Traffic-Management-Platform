# MVP validation — actual execution record

Validation date: **14 September 2026**. The existing backend was inspected before changes, retained, built, and extended in place. No Git repository, commit, remote or GitHub operation was created.

## Automated results

| Suite | H2 local database | PostgreSQL 18 isolated database |
|---|---|---|
| AccountIntegrationTest | 2 passed | 2 passed |
| BackendApplicationTests | 1 passed | 1 passed |
| GatewayIntegrationTest | 9 passed | 9 passed |
| TrafficPrimitivesTest | 7 passed | 7 passed |
| **Total** | **19 passed; 0 failures/errors** | **19 passed; 0 failures/errors** |

Evidence: [H2 summary](h2-test-summary.txt), [PostgreSQL summary](postgres-test-summary.txt). The integration tests contain multiple related assertions per lifecycle, rather than one trivial test per endpoint. Recorded elapsed times can include machine suspension during this build session and are not performance measurements.

Maven `verify` completed successfully on the final local application. The frontend and demo-service JavaScript were syntax checked with Node. PostgreSQL testing used separately initialized project-local storage on port 55432 with a generated SCRAM password; the pre-existing PostgreSQL Windows service and its databases were not modified. That temporary database server was shut down after testing, with its project-local data retained.

## Acceptance coverage

| Requested behavior | Actual result / evidence |
|---|---|
| Register, BCrypt storage, login and authenticated profile | Passed HTTP account lifecycle; stored value checked to be BCrypt, never plain password |
| Logout invalidates presented session | Passed HTTP test and both full-stack smoke runs |
| Change password rejects old password and revokes sessions | Passed account lifecycle |
| Forgot/reset password, new-password login, token reuse rejection | Passed real HTTP flow using the private test mailbox |
| Expired reset token and expired session rejection | Passed valid-token expiry tests |
| ADMIN vs OPERATOR vs DEVELOPER vs USER | Passed distinct backend role tests; basic USER cannot use gateway/create keys, OPERATOR can inspect operations |
| Administrator user list hides password/hash | Passed response inspection; browser inspected actual user list and configuration dialog |
| Forced password reset blocks login and invalidates sessions | Passed administrator HTTP action and follow-up authorization checks |
| Dynamic route create/update/disable/delete | Passed HTTP configuration and immediate forwarding behavior tests |
| Real proxy to user/product/order services | Passed native chaos checks and native/container smoke checks |
| Request/correlation IDs | Passed response header, downstream body and persisted request checks |
| Round robin | Passed exact unit sequence and live two-instance distribution |
| Least connections | Passed selection/reservation tests, 5,000 concurrent acquire/release operations, and an HTTP request using the configured strategy |
| Service failure detection and healthy-only routing | Passed stopped HTTP test-server exclusion and live controlled health failure |
| Instance recovery | Passed restart/re-registration test and live health recovery |
| GLOBAL/IP/USER/KEY/ROUTE token buckets | Passed separate HTTP capacity/rejection tests for every scope |
| Concurrent token consumption | 1,000 contenders against a 100-token fixed-time bucket admitted exactly 100 |
| 429 and Retry-After | Passed integration tests; actual load runs recorded expected rejections |
| Safe retry and no POST retry | Passed unit, HTTP integration and live failure tests |
| Overall timeout returns 504 | Passed live delayed-upstream test |
| Circuit open/block/half-open/closed behavior | Passed unit and HTTP/live recovery tests; single-probe admission asserted |
| Late in-flight success cannot overwrite new circuit | Passed generation-isolation unit test |
| API key creation, expiry, revocation and cross-user access rejection | Passed HTTP/security tests and smoke runs |
| Key rotation | Passed native smoke: old secret rejected immediately and the new secret accepted; dedicated rotation-race/load testing remains additional hardening |
| User usage isolation | User B sees zero requests when only User A generated traffic; non-owner key mutation returns 404 |
| Real metrics/logs/quota | Passed HTTP aggregate/log/quota checks and visual inspection after browser-generated and load-test traffic |
| Admin and developer panels | Both rendered and signed in using their actual seeded credentials |
| Developer API explorer | Browser sent a real request: HTTP 200, downstream instance, quota and request ID displayed |
| Responsive layout | Developer dashboard inspected at 390 × 844 and normal desktop viewport; navigation scrolls on mobile, cards remain readable |
| Native clean startup and restart | Startup/stop/restart executed; persistent account/telemetry data retained |
| Docker configuration/build/start | Compose validation, all images built, PostgreSQL healthy and all four instances probed healthy |
| Docker account/API end-to-end | Nine smoke groups passed; [container evidence](validation/docker-smoke.json), [service snapshot](validation/docker-services.txt) |
| Load testing | Actual 100, 1,000, 5,000 and 10,000 request stages, with and without configured rate policies; raw JSON and CPU/memory samples retained |
| Documentation | API reference, 16 architecture/lifecycle/deployment diagrams, security model, Windows/Docker startup and future AWS design included |

Live failure evidence: [chaos results](performance/chaos-results.json). Native smoke evidence: [native results](validation/native-smoke.json). Benchmark evidence and interpretation: [performance report](performance/README.md).

## Issues encountered and resolved

- The Oracle PATH Java launcher returned no useful output. The installed OpenJDK 25 was selected without system-wide settings changes.
- Spring Boot 4/Jackson 3 rejected an obsolete date-format property; it was removed and the application retested.
- An initial assertion expected eight gateway requests where the scenario actually sent seven; corrected to the actual seven-request scenario.
- Windows process state JSON parsed timestamps differently than the initial shutdown comparison expected. Timestamp comparison and process-exit waiting were corrected; restart was then verified. Startup now checks occupied ports before launching.
- After loopback binding was tightened, the localhost readiness probe was unreliable. It now checks 127.0.0.1 directly with proxy bypass; the final startup command completed successfully.
- Docker was initially stopped. Docker Desktop was started, the engine became available, and actual images and containers were then built and tested.

## Scope and honest limitations

This is a functional **local portfolio MVP**, not a production certification. Database configuration and sessions persist, while rate buckets, connection reservations, health observations and circuits are per gateway process. The independently running downstream services demonstrate distribution; globally coordinated multi-gateway traffic state is deferred to P1.

The local reset mailbox is functional, but external email delivery is not integrated. Demo business records are intentionally process-local. The API supports bounded ordinary HTTP bodies and a documented header allowlist, not unrestricted streaming or WebSocket proxying. Rate policies have one configuration per scope with separate buckets per identity. Retry statuses are the fixed safe transient set. Cache policy is deferred.

The dashboards use real persisted data, with explicit sample limits: latest 1,000 logs in the browser; latest 10,000 requests for p95/chart/throughput; 30-day request-log retention. Deliberate test failures and rate-limit rejections remain visible and must not be interpreted as unexplained production incidents. The reported throughput is only a local workload observation, not a claimed production limit.

No Redis/shared rate state, response caching, weighted balancing, Prometheus/Grafana, distributed tracing, Testcontainers, canary routing, Kubernetes or actual AWS deployment is claimed. Production email, broader overload/soak testing, a strict production CSP/ingress policy, DNS/egress isolation and multi-node coordination remain deployment work. See [security boundaries](security/README.md) and [AWS prerequisites](deployment/aws.md).

## Reproduce

```powershell
.\scripts\start-dev.ps1
.\scripts\run-tests.ps1
node tests/integration/smoke.mjs
node tests/chaos/run.mjs
.\scripts\run-load-test.ps1
.\scripts\run-load-test.ps1 -Unthrottled
.\scripts\stop-dev.ps1
docker compose up --build -d
$env:SMOKE_LABEL='docker'
node tests/integration/smoke.mjs
docker compose down
```

For the PostgreSQL test variant, create a **fresh dedicated test database** and set `TEST_DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD` in the test process before running Maven tests. Test contexts deliberately reuse fixed account emails; do not point tests at an existing application database. Do not delete user/application databases to rerun tests.
