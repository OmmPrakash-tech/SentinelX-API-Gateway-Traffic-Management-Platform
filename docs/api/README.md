# HTTP API reference

Base URL: `http://localhost:8080`. The UI is served at `/` and uses hash routes. Account/management APIs use JSON. Sessions use `Authorization: Bearer <opaque-token>`; there is no JWT or refresh-token protocol. Sessions last eight hours and are checked against the database on each authenticated request.

Generated IDs are UUIDs. Timestamps represent server-recorded events. Ordinary responses never expose password hashes, session hashes, reset hashes or API-key hashes.

## Public and account endpoints

| Method | Path | Body / behavior |
|---|---|---|
| GET | `/health` | Database-backed gateway health; `{status:"UP",application:"SentinelX"}` |
| POST | `/api/auth/register` | `name,email,password,confirmPassword`; creates DEVELOPER, returns public profile |
| POST | `/api/auth/login` | `email,password`; returns token, expiry and public profile |
| POST | `/api/auth/forgot-password` | `email`; generic response, independent of account existence |
| POST | `/api/auth/reset-password` | `token,password,confirmPassword`; single-use reset |
| POST | `/api/auth/logout` | Revoke the presented session |
| POST | `/api/auth/logout-all` | Revoke all caller sessions |
| POST | `/api/auth/change-password` | `currentPassword,password,confirmPassword`; revoke all sessions |
| GET | `/api/me` | Current public profile |
| PATCH | `/api/me` | `name`; email and role cannot be self-updated |
| GET | `/api/me/sessions` | Creation and expiry of caller's active sessions; no tokens |

Registration example:

```json
{"name":"Example Developer","email":"example@local.test","password":"Example-only-Password!","confirmPassword":"Example-only-Password!"}
```

Login response shape:

```json
{"token":"ONE_TIME_RESPONSE_TOKEN","expiresAt":"2026-09-14T18:00:00Z","user":{"id":"USER_UUID","name":"Example Developer","email":"example@local.test","role":"DEVELOPER","active":true,"api_enabled":true,"reset_required":false,"created_at":"...","updated_at":"...","last_login":"..."}}
```

The token above is a placeholder, not an actual credential. Public auth actions have a local per-IP bucket of ten attempts, replenishing one attempt per five seconds. Passwords require 12 characters minimum and 72 UTF-8 bytes maximum (BCrypt input bound), with matching confirmation.

## Developer resources — ownership enforced

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/me/keys` | Own key metadata and recorded usage |
| POST | `/api/me/keys` | `{"name":"Local integration","days":90}`; expiry 1–365 days |
| PATCH | `/api/me/keys/{id}` | `{"active":false}`; only non-revoked own keys |
| DELETE | `/api/me/keys/{id}` | Permanently revoke own credential |
| POST | `/api/me/keys/{id}/rotate` | Revoke old credential and return one new secret, 90-day lifetime |
| GET | `/api/me/apis` | Enabled route path/method/service catalog |
| GET | `/api/me/metrics` | Own retained request aggregates and sampled chart/p95 |
| GET | `/api/me/requests` | Latest 1,000 own request records |
| GET | `/api/me/quotas` | Current computed USER and own active KEY token-bucket availability |

Key creation/rotation returns `{id,secret,message}` once. Subsequent reads include `prefix`, state, tier, creation/expiry and usage; no secret. A resource not owned by the caller is returned as 404.

## Administrator — ADMIN only

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/admin/users` | Latest 1,000 public profiles with request and active-key counts |
| PATCH | `/api/admin/users/{id}` | `{"role":"DEVELOPER","active":true,"apiEnabled":true}`; sessions revoked |
| POST | `/api/admin/users/{id}/force-logout` | Invalidate sessions |
| POST | `/api/admin/users/{id}/force-reset` | Invalidate sessions/reset links; block login until new password |
| GET | `/api/admin/consumers` | All consumer key metadata, owner email and usage |
| PATCH | `/api/admin/consumers/{id}` | `{"tier":"STANDARD","active":true}` or ELEVATED |
| DELETE | `/api/admin/consumers/{id}` | Revoke API access for that key |
| GET | `/api/admin/audit` | Latest 1,000 administrative audit events |

Administrators cannot demote or deactivate their own administrator account. Forced reset does not invent a new password or disclose an old one: the user must request and complete recovery.

## Operations — ADMIN and OPERATOR reads

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/ops/services` | Registered services |
| GET | `/api/ops/instances` | Instance origins, measured health, latency, failures, checks and active reservations |
| GET | `/api/ops/routes` | Complete persisted routes and policies |
| GET | `/api/ops/rate-limits` | Configured scope policies |
| GET | `/api/ops/circuits` | Actual instantiated circuit states and transition counts |
| POST | `/api/ops/check-health` | Run immediate active checks (permitted operator action) |
| GET | `/api/ops/metrics` | Real retained traffic, p95 sample, health, consumers, retries and security-event counts |
| GET | `/api/ops/logs` | Latest 1,000 request logs |

OPERATOR cannot manage users, credentials, or persistent routing configuration. USER has basic profile, security and own historical usage access, but cannot use the gateway or create/rotate API credentials. DEVELOPER can create/rotate own API keys and use the gateway. OPERATOR can inspect operations and exercise gateway requests with its session, but cannot create/rotate API credentials. ADMIN has all of those capabilities.

## Configuration mutations — ADMIN only

Services: POST `/api/admin/services`, PUT/DELETE `/api/admin/services/{id}`.

```json
{"name":"user-service","enabled":true,"strategy":"LEAST_CONNECTIONS"}
```

Strategies: ROUND_ROBIN or LEAST_CONNECTIONS. Deletion requires removal of referencing routes/instances first; foreign keys reject unsafe deletion with 409.

Instances: POST `/api/admin/instances`, DELETE `/api/admin/instances/{id}`.

```json
{"serviceId":"SERVICE_UUID","baseUrl":"http://localhost:9101","healthPath":"/health","enabled":true}
```

Only origins with an exact hostname in `UPSTREAM_HOSTS` are accepted; no URL credentials, query strings, fragments or base paths. Health paths are simple absolute paths. Initial health is UNKNOWN until checked; failed checks immediately exclude an instance and successful checks restore it.

Routes: POST `/api/admin/routes`, PUT/DELETE `/api/admin/routes/{id}`.

```json
{"pathPrefix":"/users","methods":"GET,POST","serviceId":"SERVICE_UUID","enabled":true,"timeoutMs":3000,"connectTimeoutMs":1000,"readTimeoutMs":2000,"retries":1,"backoffMs":50,"failureThreshold":5,"recoveryMs":10000}
```

All fields are required. Prefixes match full path segments, most specific first. Limits: total 100–30,000 ms; connect 100–10,000 ms; body 100–30,000 ms; retries 0–3; initial backoff 0–1,000 ms; consecutive-failure threshold 1–100; recovery interval 100–300,000 ms. Retryable statuses are fixed to 502, 503, 504, with GET/HEAD/OPTIONS only. Cache policy and custom retry status lists are deferred.

Rate policies: POST `/api/admin/rate-limits`, PUT/DELETE `/api/admin/rate-limits/{id}`.

```json
{"scope":"KEY","capacity":100,"refillPerSecond":10,"enabled":true}
```

One policy per scope: GLOBAL, IP, USER, KEY, ROUTE. Capacity 1–100,000; refill 0.001–100,000 tokens/s. ELEVATED keys double only the KEY capacity/refill. Every applicable policy must admit traffic. Tokens spent by an earlier scope are not refunded if a later scope rejects the request. Editing capacity/refill starts a fresh bucket. Buckets are local to the current gateway, and IP uses the direct socket address, not client-controlled forwarding headers.

## Gateway

`ANY /gateway/{registered-prefix}/...`: authenticate by API key or bearer session. `/gateway` is stripped before forwarding. Query strings and accepted HTTP bodies are preserved; encoded/dot-segment/noncanonical paths are rejected. The request method must be enabled on the selected route.

```powershell
Invoke-RestMethod http://localhost:8080/gateway/users -Headers @{'X-API-Key'='YOUR_KEY'}
```

`X-Request-ID` is generated or accepted if it is 1–64 ASCII letters/digits/dot/underscore/hyphen. Credentials and hop-by-hop headers are not forwarded. The request allowlist is Accept, Content-Type, Accept-Language, If-None-Match and If-Modified-Since; the response allowlist is Content-Type, ETag, Last-Modified, Cache-Control, Content-Language and X-Instance-ID. Gateway credentials never reach demo services.

Gateway response headers include `X-Request-ID`, `X-SentinelX-Instance`, `X-SentinelX-Retries`, and `X-RateLimit-{SCOPE}-Limit/Remaining` for evaluated scopes. A rejected policy includes `Retry-After`. Maximum request body: 1 MiB. Upstream buffering is bounded to 8 MiB. Redirects are not followed. WebSockets, streaming, file uploads and arbitrary header pass-through are not part of this MVP.

## Error format

```json
{"timestamp":"2026-09-14T10:00:00Z","status":429,"error":"RATE_LIMIT_EXCEEDED","message":"Request rate limit exceeded (KEY)","requestId":"example-request-id"}
```

400 invalid fields/reset/path; 401 missing, expired or revoked credentials; 403 role or API-access restriction; 404 missing route/resource; 409 conflicting/dependent record; 413 oversized request; 429 rate limit; 502 upstream connection/body error; 503 no healthy capacity/open circuit; 504 deadline. Expected application errors contain no stack trace. Unexpected framework/container errors may use the framework's own error response.
