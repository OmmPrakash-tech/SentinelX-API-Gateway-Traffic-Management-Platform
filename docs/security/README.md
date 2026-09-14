# Security model and deployment boundaries

## Authentication

Passwords are BCrypt-hashed at cost 12. The input limit avoids BCrypt's 72-byte truncation. The API requires current-password verification for a change and matching confirmation for a new password. Password hashes are never selected into public profile, admin or key-list responses. A dummy BCrypt check is used for unknown login accounts.

Sessions are random 256-bit opaque bearer tokens. Only SHA-256 token hashes are stored, with an eight-hour expiry and a user foreign key. Each request checks the current account state and role. Logout deletes the presented session; password changes, resets, administrator role/status updates and force-logout revoke sessions. There is no JWT that stays valid after revocation.

The browser stores the bearer token in sessionStorage, scoped to its tab. No ambient authentication cookies are used; CSRF is disabled for this explicit-header API model. This does not protect a token from same-origin XSS. Dynamic text is escaped, responses are rendered as text, and the application does not insert untrusted HTML. Spring Security supplies content-type, frame and cache headers. TLS and a reviewed CSP must be applied before Internet exposure. The current stylesheet fetches optional Google Fonts; system fonts work as fallback.

Public authentication actions are protected by a process-local per-IP token bucket. This is a demonstration defense, not an Internet-scale anti-abuse service. For deployment, add shared abuse detection and trusted ingress rate controls.

## Authorization and isolation

ADMIN-only URLs protect mutations of users, services, instances, routes, policies and consumers. ADMIN and OPERATOR can inspect operations and explicitly trigger health checks. All other roles can access only authenticated self-service endpoints. USER cannot call the gateway or create/rotate API keys. DEVELOPER can do both; OPERATOR can inspect operations and run session-authenticated gateway checks but cannot create/rotate credentials. ADMIN has full access.

Key IDs are never sufficient authority: ownership is checked using both key ID and authenticated user ID. Usage/log queries bind the caller's ID on the backend. APIs do not accept a caller-provided owner ID. Admin APIs list safe metadata only. Account and key revocation are checked on subsequent gateway requests. An already-authorized in-flight request can finish; this is not a cancellation protocol.

## Reset links

Reset tokens are independently generated, SHA-256 hashed, valid for 15 minutes and consumed under database locking. Resetting updates the password, consumes other reset links and revokes sessions in the same transaction. Forced reset invalidates previous links and blocks login until recovery completes. Forgot-password responses are intentionally generic.

The local mailbox is explicit development-only delivery. Raw links exist there just as they would in an email inbox; never expose this folder over HTTP. By default production configuration has no mailbox adapter. Integrate authenticated transactional email delivery and trustworthy public reset-link origins before external deployment. No real emails are sent by this project.

## API keys

Keys contain 256 bits of random secret material and an `sx_` prefix. The database stores a hash and a short display prefix. Creation/rotation responses show the new secret once. Rotation revokes the old key in a transaction; revoked credentials are not reactivated. Expiry, owner activation, API-access flag and reset-required state are enforced at validation time.

## Upstream isolation

Only ADMIN can register an origin. Hostnames must match `UPSTREAM_HOSTS` exactly. Only HTTP/HTTPS are accepted, no credentials/base path/query/fragment. Automatic redirects are disabled. Origins are checked again at dispatch and health probing. Loopback is allowed in local development because the demo processes run there.

This is an allowlist, not a complete DNS-rebinding or network-egress sandbox. Restrict the deployment host allowlist, DNS administration and network egress. Block cloud metadata endpoints and unrelated internal systems at the network layer. Do not allow an untrusted party to control an allowed DNS name.

Incoming Authorization, X-API-Key, Cookie, Host and hop-by-hop headers are not forwarded. Only documented request and response headers pass through. X-Request-ID is constrained and propagated; query strings are proxied but omitted from logs. Request bodies are bounded at 1 MiB and upstream buffering at 8 MiB. Unsafe path encodings, dot segments and duplicate separators are rejected. Direct client IP is used; untrusted X-Forwarded-For is ignored.

## Concurrency and resilience

Token consumption and balancer reservation/release are synchronized; state maps are concurrent. Active counts are released in a finally block. Circuit half-open admission permits one probe; generation tracking prevents a response from an old request closing a newer circuit. HTTP connection and response/overall deadlines bound calls; safe-method retries share one overall time budget. No automatic retry of POST/PUT/PATCH/DELETE is performed.

Traffic-control state is local to the gateway process and resets on restart. This is documented rather than presented as cluster-wide coordination. The shared relational persistence enables configuration/session persistence but does not make in-memory circuit or rate state distributed.

## Telemetry and retained data

Request logs contain correlation ID, user/key IDs, method, path, target, status, latency, retries and time. They never contain passwords, secrets, bodies or query strings. Paths can themselves contain user-supplied identifiers; configure retention and access appropriately. Public auth failures are stored as events without submitted email/password. Administrative audit records contain actor/action/resource/time/correlation, not snapshots of secrets.

Gateway log retention is 30 days. Audit and system-event records currently require an operator-defined retention/archival policy for long-lived deployments. There is no asynchronous durable spool during database failure; log persistence is coupled to the gateway database. Large-scale telemetry should be moved to a bounded asynchronous pipeline in a later phase.

## Deployment checklist tied to this implementation

- Disable demo seed and local mailbox before exposure; bootstrap a real administrator through a controlled provisioning process.
- Supply a unique database credential through secrets management; use least-privilege migration/runtime accounts.
- Use TLS at trusted ingress, private downstream networking and restrictive egress rules.
- Back up PostgreSQL and test restores; file-backed H2 is for local development.
- Add production recovery delivery, centralized security controls and shared traffic state before multi-instance scaling.
- Review data retention, frontend CSP, pagination limits, overload handling and observability for the intended workload.

The integration and concurrency tests provide evidence for specific behaviors; they are not a security audit or production-readiness certification.
