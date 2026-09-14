# SENTINELX — COMPLETE END-TO-END INDUSTRY-GRADE PROJECT BUILD PROMPT

You are acting as a senior software architect, Java/Spring Boot engineer, distributed-systems engineer, frontend engineer, security engineer, database engineer, DevOps engineer, and QA engineer.

Build a complete, professional, production-style portfolio project called:

# SentinelX

## Distributed API Gateway & Intelligent Traffic Management Platform

Project directory:

`D:\projects\sentineIX`

There is already an existing backend directory:

`D:\projects\sentineIX\backend`

The backend is newly created and may contain an initial implementation.

# CRITICAL INSTRUCTION

Before doing anything, inspect the entire existing project and understand what is already present.

DO NOT delete the existing backend.

DO NOT blindly recreate the backend.

DO NOT replace useful existing files just because a different implementation is easier.

Reuse and improve existing code wherever practical.

---

# 1. ENVIRONMENT-FIRST RULE

First inspect the development environment and project.

Check:

* `D:\projects\sentineIX`
* `D:\projects\sentineIX\backend`
* existing source files
* package/build files
* configuration
* dependencies
* environment files
* Java availability
* Maven/Gradle availability
* Node.js availability
* npm/pnpm/yarn availability
* Docker availability
* PostgreSQL availability
* Redis availability
* other relevant locally installed tools

Determine which technologies already exist and prefer those technologies.

## TECHNOLOGY VERSION RULE

Do NOT arbitrarily upgrade or downgrade versions.

Do NOT force a particular technology version.

Use compatible technologies and versions already available on the system whenever practical.

If a required component is missing, choose the most compatible option with the current environment.

Do not make unnecessary system-wide changes.

---

# 2. GIT REQUIREMENT

DO NOT initialize Git.

DO NOT run:

`git init`

DO NOT create a GitHub repository.

DO NOT connect this project to GitHub.

DO NOT push anything.

DO NOT create commits.

DO NOT perform GitHub actions.

ONLY create a comprehensive `.gitignore` at:

`D:\projects\sentineIX\.gitignore`

The `.gitignore` must exclude, as appropriate:

* `.env`
* secrets
* credentials
* API keys
* JWT secrets
* passwords
* IDE files
* build artifacts
* logs
* generated files
* temporary files
* `node_modules`
* local infrastructure data
* database dumps
* OS-specific temporary files

---

# 3. PROJECT OBJECTIVE

Build SentinelX as a real distributed API gateway and traffic-management platform.

The system should demonstrate strong engineering skills in:

* Java
* Spring Boot
* REST APIs
* HTTP
* networking concepts
* service discovery
* dynamic routing
* load balancing
* rate limiting
* concurrency
* fault tolerance
* authentication
* authorization
* API key management
* database engineering
* caching
* observability
* structured logging
* metrics
* distributed systems
* automated testing
* Docker
* AWS-ready architecture

The system must be genuinely functional.

Do not build fake dashboards.

Do not use hard-coded fake metrics.

Do not fake service-health status.

Do not claim performance numbers that were not measured.

---

# 4. HIGH-LEVEL SYSTEM

The intended flow is:

Client
↓
SentinelX Gateway
↓
Authentication / API Key validation
↓
Authorization
↓
Request ID generation
↓
Rate Limiting
↓
Route Resolution
↓
Service Discovery
↓
Health Check Filtering
↓
Load Balancing
↓
Circuit Breaker
↓
Retry where appropriate
↓
Backend Service
↓
Response
↓
Metrics + Logs + Tracing

Conceptual architecture:

```text
                    ┌──────────────────────────┐
                    │       User Panel         │
                    │    Developer Portal      │
                    └────────────┬─────────────┘
                                 │
                                 │
                    ┌────────────▼─────────────┐
                    │      Admin Panel         │
                    │   Operations Console     │
                    └────────────┬─────────────┘
                                 │
                                 ▼
                      ┌────────────────────┐
                      │   SentinelX        │
                      │    API Gateway     │
                      └─────────┬──────────┘
                                │
        ┌───────────────────────┼─────────────────────────┐
        │                       │                         │
        ▼                       ▼                         ▼
 Authentication            Rate Limiting             Route Manager
        │                       │                         │
        └───────────────────────┼─────────────────────────┘
                                │
                                ▼
                         Service Registry
                                │
                                ▼
                          Load Balancer
                       /        |        \
                      /         |         \
                     ▼          ▼          ▼
               User Service Product Service Order Service
                     │          │          │
                     └──────────┼──────────┘
                                │
                         Database / Cache
                                │
                         Metrics / Logs
                                │
                         Monitoring Layer
```

---

# 5. EXPLICIT PROJECT SCOPE

SentinelX is primarily:

## A Distributed API Gateway & Traffic Management Platform

It is NOT primarily:

* an e-commerce application
* a social network
* a generic CRUD application
* an AI chatbot
* an ML project
* a payment application

The demo services are only supporting services used to demonstrate SentinelX.

Prioritize:

1. correctness
2. working end-to-end architecture
3. security
4. concurrency safety
5. fault tolerance
6. observability
7. testing
8. maintainability
9. frontend quality
10. advanced features

Do not add unnecessary complexity just to increase the number of technologies.

---

# 6. MVP PRIORITIES

## P0 — MUST HAVE

The MVP is not complete until all of these work:

1. User registration
2. Secure authentication
3. Login/logout
4. Change password
5. Forgot password
6. Reset password
7. Role-based authorization
8. Admin panel
9. User/developer panel
10. User management
11. API key management
12. API gateway
13. Dynamic routing
14. Service registry
15. Health checks
16. Round Robin load balancing
17. Least Connections load balancing
18. Token Bucket rate limiting
19. Circuit breaker
20. Retry mechanism
21. Request/correlation ID
22. Structured logs
23. Real metrics
24. Database persistence
25. Demo services
26. Automated tests
27. Docker-based local development
28. `.gitignore`
29. Complete documentation

---

# 7. P1 — IMPLEMENT AFTER P0

Only start these after P0 is working and tested:

* distributed rate limiting using shared state
* weighted load balancing
* additional rate-limit algorithms
* Redis caching
* detailed API analytics
* audit-log interface
* Prometheus integration
* Grafana dashboard
* distributed tracing
* Testcontainers
* advanced developer console
* advanced service configuration
* load-test automation
* chaos-test automation

---

# 8. P2 — OPTIONAL ADVANCED FEATURES

Only implement after P0/P1 are stable:

* weighted routing
* canary releases
* blue/green routing
* dynamic configuration reload
* intelligent traffic routing
* anomaly detection
* adaptive rate limiting
* advanced request transformation
* WebSocket proxying
* autoscaling support
* AWS deployment automation
* infrastructure-as-code
* multi-region architecture

Do not allow P2 work to delay MVP completion.

---

# 9. EXPLICITLY OUT OF INITIAL SCOPE

Do not implement these during the initial MVP unless they are already present and useful:

* Kubernetes
* service mesh
* multi-region deployment
* multi-cloud
* custom distributed database
* custom message broker
* custom authentication protocol
* mobile application
* complex payment system
* enterprise SSO
* social login
* complex billing
* elaborate ML pipeline
* unnecessary AI functionality

---

# 10. ACCOUNT AND AUTHENTICATION SYSTEM

Create a complete authentication/account management system.

The system must support:

* registration
* login
* logout
* secure password storage
* change password
* forgot password
* reset password
* session/token invalidation
* account activation/deactivation
* role-based access
* profile management
* last-login information
* password reset history where appropriate

---

# 11. PASSWORD STORAGE — IMPORTANT

NEVER store passwords in plaintext.

The database must contain a field such as:

`password_hash`

and store only a secure one-way password hash.

The actual password must never be:

* stored in plaintext
* returned in API responses
* shown in the admin frontend
* logged
* included in JWTs
* written to application logs
* committed to source code

During login:

```text
User enters password
       ↓
Authentication service
       ↓
Secure password verification
       ↓
Stored password hash comparison
       ↓
Authentication success/failure
```

For PostgreSQL, the users table can contain:

```text
id
name
email
password_hash
role
status
created_at
updated_at
last_login
```

The `password_hash` should be visible to backend/database administrators only where legitimate database access exists, but it must not be exposed through ordinary application APIs or the admin UI.

---

# 12. USER PASSWORD MANAGEMENT

The User Panel must provide:

## Change Password

Require:

* current password
* new password
* confirmation of new password

After successful password change:

* invalidate existing authentication sessions/tokens where appropriate
* require fresh authentication where appropriate
* never reveal the old password

---

## Forgot Password

Implement:

```text
User selects "Forgot Password"
        ↓
Enters email
        ↓
System generates secure reset token
        ↓
Reset mechanism
        ↓
User creates new password
        ↓
Password is securely hashed
        ↓
Old password becomes invalid
        ↓
Existing sessions/tokens are invalidated where appropriate
```

Do not reveal whether an email/account exists through insecure responses.

For local development, provide a practical documented way to test password reset without requiring a production email service.

---

## Reset Password

Require:

* valid reset token
* new password
* confirm password

Reset tokens must:

* expire
* be single-use
* be securely generated
* be invalidated after use

---

# 13. ADMIN USER MANAGEMENT

Create an industry-grade User Management area in the Admin Panel.

The admin should be able to see useful information about every user.

Display:

* User ID
* name
* email
* phone if collected
* role
* account status
* registration date
* updated date
* last login
* API consumer association
* API usage summary
* API key status
* recent activity
* password-reset status where appropriate

The admin should be able to:

* activate user
* deactivate user
* assign role
* change role according to authorization rules
* force logout/invalidate sessions
* force password reset
* inspect user activity
* inspect usage
* disable API access

## PASSWORD RESTRICTION

The admin must NOT be able to view the user's actual password.

The admin must NOT be shown the password hash in the normal frontend UI.

If an administrator needs to help a user who forgot their password, provide:

# Force Password Reset

rather than exposing the password.

This action should:

* invalidate current sessions/tokens
* invalidate existing reset tokens
* mark password reset required
* require the user to establish a new password

---

# 14. USER ROLES

Implement:

```text
ADMIN
OPERATOR
DEVELOPER
USER
```

## ADMIN

Full system administration.

Can:

* manage users
* manage services
* manage routes
* manage rate limits
* manage API consumers
* inspect logs
* inspect metrics
* inspect audit logs
* manage operational settings

## OPERATOR

Can perform approved operational tasks:

* inspect services
* inspect health
* inspect traffic
* inspect circuit breakers
* inspect logs
* manage selected operational settings

## DEVELOPER

Can:

* manage own API keys
* inspect own API usage
* use API explorer
* read API documentation
* manage profile/security

## USER

Basic authenticated user capabilities.

All authorization must be enforced on the backend.

Do not rely on frontend route hiding for security.

---

# 15. ADMIN PANEL

Build an industry-grade Admin Panel.

Do not make it look like a basic CRUD application.

Suggested navigation:

```text
SentinelX
│
├── Overview
├── Traffic
├── Services
├── Service Instances
├── Routes
├── Load Balancing
├── Rate Limits
├── API Consumers
├── Users
├── Circuit Breakers
├── Logs
├── Audit Logs
├── System Health
└── Settings
```

---

# 16. ADMIN OVERVIEW

Show real data:

* total requests
* requests/sec
* requests today
* success rate
* error rate
* average latency
* p95 latency if available
* healthy services
* unhealthy services
* active instances
* rate-limit violations
* open circuit breakers
* active API consumers

Do not hard-code numbers.

---

# 17. ADMIN SERVICE MANAGEMENT

Admin can:

* create/register service
* view services
* edit service
* enable service
* disable service
* delete service where appropriate
* inspect instances
* inspect health
* inspect latency
* inspect failure count
* inspect last heartbeat

Example:

```text
Product Service

Instance 1   HEALTHY
Instance 2   HEALTHY
Instance 3   UNHEALTHY
```

---

# 18. ADMIN ROUTE MANAGEMENT

Create dynamic route management.

Examples:

```text
/api/users/**      → user-service
/api/products/**   → product-service
/api/orders/**     → order-service
```

Route configuration should support:

* route ID
* path pattern
* methods
* target service
* enabled/disabled
* timeout
* retry policy
* rate-limit policy
* cache policy
* circuit-breaker policy

Do not hard-code routing logic throughout controllers.

---

# 19. ADMIN LOAD BALANCING

Implement:

## Round Robin

Request sequence:

```text
A
B
C
A
B
C
```

## Least Connections

Send traffic toward the healthy backend instance currently handling the fewest active requests.

Only healthy instances can receive traffic.

Keep the strategy architecture extensible.

---

# 20. ADMIN RATE-LIMIT MANAGEMENT

MVP algorithm:

# Token Bucket

Support:

* global limits
* per-IP
* per-user
* per-API-key
* per-route

When the limit is exceeded:

Return:

`HTTP 429`

Record a real violation event.

Dashboard should show:

* configured limit
* current usage
* remaining quota
* violations
* top consumers

---

# 21. API KEY MANAGEMENT

API consumers should support:

* create
* activate
* deactivate
* revoke
* rotate
* inspect metadata
* assign rate-limit tier
* usage statistics

Store API credentials securely.

Do not unnecessarily store raw API secrets.

---

# 22. USER PANEL

Create a second polished application experience.

Suggested navigation:

```text
SentinelX Developer Portal

├── Dashboard
├── My APIs
├── API Keys
├── Usage
├── Requests
├── API Explorer
├── Documentation
├── Profile
└── Security
```

---

# 23. USER DASHBOARD

Show only the authenticated user's information.

Display:

* requests today
* total API requests
* success rate
* errors
* average latency
* current quota
* remaining quota
* API keys
* recent requests
* recent failures

Never show another user's information.

---

# 24. USER PROFILE

Allow user to:

* update name
* update profile information
* change allowed account information
* view account status
* view registration date
* view last login

---

# 25. USER SECURITY PAGE

Provide:

* change password
* forgot-password information
* reset-password flow
* active session/token information where practical
* logout
* force logout from other sessions where practical

---

# 26. USER API KEY PAGE

Allow:

* create API key
* revoke API key
* rotate API key
* inspect status
* inspect created date
* inspect expiry
* inspect usage

Do not expose old revoked secrets.

---

# 27. USER API USAGE PAGE

Show real user-specific data:

* requests over time
* successful requests
* failures
* latency
* endpoints used
* rate-limit violations
* recent requests

Ensure strict user-level data isolation.

---

# 28. USER API EXPLORER

Build a developer-console experience.

The user can:

1. select API
2. select method
3. enter request data
4. send request
5. see response status
6. see response body
7. see latency
8. see request ID

The request must actually go through SentinelX.

No fake API responses.

---

# 29. API GATEWAY CORE

The gateway must:

* accept HTTP requests
* create/propagate request IDs
* authenticate
* authorize
* identify route
* apply rate limiting
* resolve target service
* check service health
* select healthy backend
* apply circuit breaker
* retry allowed transient failures
* enforce timeout
* forward request
* collect metrics
* generate structured logs
* return appropriate response

---

# 30. SERVICE REGISTRY

Each service instance should contain:

* service ID
* service name
* host
* port
* health endpoint
* status
* registration time
* last heartbeat
* metadata
* weight if later supported

Support:

* registration
* deregistration
* heartbeat
* health checks
* unhealthy detection
* recovery

---

# 31. HEALTH CHECKING

Periodically check each service.

Track:

* state
* latency
* consecutive failures
* successful checks
* last check
* last failure

Behavior:

```text
HEALTHY
   ↓
Repeated failures
   ↓
UNHEALTHY
   ↓
Removed from routing
```

Recovery:

```text
UNHEALTHY
   ↓
Successful health checks
   ↓
HEALTHY
   ↓
Return to routing pool
```

---

# 32. CIRCUIT BREAKER

Implement:

```text
CLOSED
OPEN
HALF_OPEN
```

Flow:

```text
Backend repeatedly fails
        ↓
Failure threshold reached
        ↓
CIRCUIT OPEN
        ↓
Stop sending normal traffic
        ↓
Recovery interval
        ↓
HALF_OPEN
        ↓
Test request succeeds
        ↓
CLOSED
```

Track:

* failure count
* threshold
* state
* timestamps
* transition events

---

# 33. RETRY SYSTEM

Implement controlled retry.

Retry only appropriate transient failures.

Support:

* retry count
* backoff
* retryable status codes
* timeout
* route-level configuration

Do not retry every request.

Be careful with non-idempotent operations.

---

# 34. REQUEST TIMEOUTS

Support route-level:

* connection timeout
* read timeout
* overall request timeout

Return meaningful errors when timeout occurs.

Record timeout metrics.

---

# 35. REQUEST PRIORITY

Keep as P2 unless it is simple to add later.

Potential levels:

```text
CRITICAL
HIGH
NORMAL
LOW
```

Do not allow this feature to delay core gateway functionality.

---

# 36. CACHING

Implement as P1.

Use caching primarily for safe read-heavy requests.

Potential examples:

```text
GET /api/products/{id}
```

Track:

* cache hit
* cache miss
* TTL
* invalidation

Do not cache sensitive responses by default.

---

# 37. REQUEST CORRELATION

Every request should have:

`X-Request-ID`

If the client does not provide one, generate it.

Propagate it where appropriate.

The ID must appear in:

* logs
* errors
* service requests
* audit events
* tracing/metrics metadata where applicable

---

# 38. OBSERVABILITY

Track real:

* request count
* throughput
* 2xx
* 4xx
* 5xx
* latency
* p95 if available
* active requests
* rate-limit violations
* retry events
* circuit transitions
* health state
* cache metrics where implemented
* authentication failures

---

# 39. STRUCTURED LOGGING

Use structured logs containing relevant fields such as:

```text
timestamp
requestId
userId where appropriate
apiConsumerId
method
path
targetService
status
latency
eventType
errorCode
```

Never log:

* passwords
* password hashes
* raw API secrets
* authentication tokens
* private credentials

---

# 40. AUDIT LOGGING

Record administrative security-sensitive operations.

Examples:

```text
ADMIN_CREATED_USER
ADMIN_DISABLED_USER
ADMIN_CHANGED_ROLE
ADMIN_FORCED_PASSWORD_RESET
ADMIN_CREATED_ROUTE
ADMIN_UPDATED_RATE_LIMIT
ADMIN_REVOKED_API_KEY
OPERATOR_DISABLED_SERVICE
```

Store:

* actor
* action
* resource
* timestamp
* request ID
* before/after data where appropriate

Do not include passwords or secrets.

---

# 41. DEMO SERVICES

Create:

## user-service

```text
GET /users
GET /users/{id}
POST /users
GET /health
```

## product-service

```text
GET /products
GET /products/{id}
POST /products
GET /health
```

## order-service

```text
GET /orders
GET /orders/{id}
POST /orders
GET /health
```

These services exist to demonstrate gateway functionality.

Keep business logic simple.

Each service should have development/test controls for:

* artificial latency
* controlled failures

These controls must be protected and available only in development/testing contexts.

---

# 42. DATABASE DESIGN

Create a proper relational design.

Possible tables:

```text
users
roles
user_roles
password_reset_tokens
refresh_sessions or equivalent session records
api_consumers
api_keys
services
service_instances
routes
rate_limit_policies
retry_policies
circuit_breaker_policies
request_logs
audit_logs
system_events
```

Use:

* primary keys
* foreign keys
* indexes
* constraints
* timestamps
* suitable relationships

Do not create one giant table.

---

# 43. PASSWORD RESET DATA

Store password-reset information safely.

Possible structure:

```text
password_reset_tokens

id
user_id
token_hash
expires_at
used_at
created_at
```

Store a secure representation of the reset token rather than unnecessary raw token persistence.

Tokens must expire and be single-use.

---

# 44. DATABASE SECURITY

Never expose:

* database passwords
* credentials
* API secrets
* JWT signing secrets
* passwords
* password hashes

through ordinary UI/API responses.

Use environment-based configuration.

---

# 45. API ERROR MODEL

Use a consistent format.

Example:

```json
{
  "timestamp": "...",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Request rate limit exceeded",
  "requestId": "..."
}
```

Never expose stack traces in production-style responses.

---

# 46. FRONTEND QUALITY BAR

Both panels must look like serious industry software.

Avoid:

* default browser forms
* plain HTML tables
* generic student-dashboard appearance
* hard-coded statistics
* huge cluttered pages

Use:

* polished navigation
* responsive layout
* cards
* charts
* tables
* search
* filters
* pagination
* status badges
* modals
* confirmations
* toasts
* loading states
* empty states
* error states
* consistent typography
* consistent branding
* strong information hierarchy

---

# 47. ADMIN VS USER VISUAL SEPARATION

The two panels should clearly communicate different responsibilities.

## Admin

Infrastructure/operations-oriented.

Focus on:

* services
* traffic
* routes
* users
* rate limits
* health
* logs
* operational controls

## User/Developer

Developer/productivity-oriented.

Focus on:

* APIs
* API keys
* documentation
* usage
* requests
* API explorer
* profile
* security

Use consistent SentinelX branding while making the purpose of each portal immediately obvious.

---

# 48. BACKEND AUTHORIZATION

Every protected backend endpoint must verify:

* authentication
* role
* resource ownership where relevant

Example:

A developer must not be able to:

`GET /api/admin/users`

A user must not be able to:

`GET /api/user/usage/{another-user-id}`

A developer must only manage their own API keys.

The frontend must never be the only authorization barrier.

---

# 49. USER DATA ISOLATION

Create tests proving:

```text
User A
  ↓
Can access own profile
Can access own usage
Can access own API keys

Cannot access User B data
Cannot access admin APIs
Cannot modify another user's API key
```

---

# 50. API KEY SECURITY

Never put raw API keys into logs.

Never expose revoked API secrets.

Prefer secure storage/hashed representation where practical.

Provide metadata rather than secrets after initial creation.

---

# 51. TESTING

Create substantial tests.

## Unit tests

Test:

* route resolution
* token bucket
* round robin
* least connections
* circuit breaker
* retry
* authentication
* authorization
* password reset validation
* API key validation

## Integration tests

Test:

```text
Gateway → User Service
Gateway → Product Service
Gateway → Order Service
Authentication → Database
Admin → Authorization
User → Data Isolation
Gateway → Service Registry
```

## Security tests

Test:

* invalid login
* wrong password
* expired authentication
* unauthorized role
* revoked API key
* invalid reset token
* expired reset token
* reused reset token
* unauthorized user-data access
* sensitive-information leakage

## Concurrency tests

Test concurrent access against:

* rate limiter
* service registry
* load balancer
* counters
* circuit breaker

---

# 52. LOAD TESTING

Create load-testing capability.

Test increasing traffic levels.

For example:

```text
100 requests
1,000 requests
5,000 requests
10,000 requests
```

Actually measure:

* throughput
* p50
* p95
* p99
* error rate
* resource usage

Never fabricate benchmark results.

Store actual results under:

`docs/performance/`

---

# 53. FAILURE TESTING

Test:

## Backend unavailable

```text
Instance A DOWN
      ↓
Health check
      ↓
Instance A removed
      ↓
Traffic continues through healthy instances
```

## Backend timeout

```text
Request
 ↓
Timeout
 ↓
Retry if policy allows
```

## Repeated failure

```text
Failures
 ↓
Circuit opens
```

## Recovery

```text
Backend returns
 ↓
HALF_OPEN
 ↓
Successful verification
 ↓
CLOSED
```

---

# 54. DOCKER

Provide a local environment containing only the infrastructure genuinely needed.

Potential components:

* gateway
* demo services
* PostgreSQL
* Redis if used
* monitoring components if implemented

Create a straightforward documented startup process.

---

# 55. WINDOWS SCRIPTS

The primary development machine is Windows.

Where useful create:

```text
scripts/start-dev.ps1
scripts/stop-dev.ps1
scripts/reset-dev.ps1
scripts/run-tests.ps1
scripts/run-load-test.ps1
```

Make sure commands match what is actually installed.

Do not assume tools that are not present.

---

# 56. PROJECT STRUCTURE

Use/adapt a structure similar to:

```text
sentineIX/
│
├── backend/
│
├── frontend/
│
├── services/
│   ├── user-service/
│   ├── product-service/
│   └── order-service/
│
├── infrastructure/
│   ├── docker/
│   ├── database/
│   ├── monitoring/
│   └── configuration/
│
├── scripts/
│
├── tests/
│   ├── integration/
│   ├── load/
│   └── chaos/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── security/
│   ├── performance/
│   └── deployment/
│
├── .gitignore
├── docker-compose.yml
└── README.md
```

Adapt this to the existing backend rather than duplicating functionality.

---

# 57. ARCHITECTURE DOCUMENTATION

Create Mermaid diagrams for:

* complete architecture
* gateway request lifecycle
* authentication
* authorization
* password-reset flow
* service discovery
* load balancing
* rate limiting
* circuit breaker
* retry
* admin architecture
* user architecture
* database ER diagram
* observability
* Docker deployment
* future AWS deployment

Store these under:

`docs/architecture/`

---

# 58. API DOCUMENTATION

Document:

* registration
* login
* logout
* change password
* forgot password
* reset password
* admin endpoints
* user endpoints
* gateway endpoints
* service-management endpoints
* route-management endpoints
* API-key endpoints
* metrics/health endpoints

Include request and response examples.

Do not include actual secrets.

---

# 59. DEVELOPMENT CREDENTIALS

Because this is a local development project, create documented development/demo accounts with known credentials.

Put them in:

`docs/development-credentials.md`

Do not hard-code sensitive real-world credentials.

The documentation may contain intentionally created local demo passwords because they are development test credentials, but production-like password handling must still use secure hashing.

Example format:

```text
ADMIN
Email: admin@sentinelx.local
Password: [development-only password]

DEVELOPER
Email: developer@sentinelx.local
Password: [development-only password]
```

Use whatever credentials are actually created by the seed/setup process.

Do not claim credentials that do not work.

---

# 60. AWS READINESS

Do not automatically deploy to AWS.

Prepare documentation for future deployment.

Potential conceptual architecture:

```text
Internet
   ↓
AWS Load Balancer
   ↓
SentinelX Gateway Instances
   ↓
Backend Services
   ↓
Managed Database
   ↓
Managed Cache/Event Infrastructure
```

Document:

* networking
* security groups
* secrets
* containers
* scaling
* database
* cache
* observability
* deployment approach

Never put real AWS credentials in the project.

---

# 61. PHASED DEVELOPMENT

Follow this order.

## PHASE 1 — INSPECTION

Inspect:

* existing source
* backend
* system environment
* installed tools

Do not immediately generate a huge amount of code.

---

## PHASE 2 — BACKEND FOUNDATION

Ensure:

* existing backend builds
* application starts
* configuration works
* database connectivity works where required

---

## PHASE 3 — DATABASE AND USER SYSTEM

Implement:

* users
* roles
* authentication
* password hashing
* registration
* login
* logout
* change password
* forgot password
* reset password
* account status
* sessions/tokens where applicable

Test this before proceeding.

---

## PHASE 4 — AUTHORIZATION

Implement role-based access.

Verify:

ADMIN ≠ OPERATOR ≠ DEVELOPER ≠ USER

---

## PHASE 5 — ADMIN PANEL

Build the complete Admin Panel.

Connect it to real backend data.

---

## PHASE 6 — USER PANEL

Build the complete User/Developer Panel.

Connect it to real backend data.

---

## PHASE 7 — GATEWAY

Implement:

* request proxy
* request IDs
* route resolution

---

## PHASE 8 — SERVICE REGISTRY

Implement:

* service registration
* instance registration
* health checks
* recovery

---

## PHASE 9 — LOAD BALANCING

Implement:

* Round Robin
* Least Connections

---

## PHASE 10 — RATE LIMITING

Implement:

* Token Bucket
* per-key
* per-user
* per-IP
* per-route

---

## PHASE 11 — CIRCUIT BREAKER

Implement:

* CLOSED
* OPEN
* HALF_OPEN

---

## PHASE 12 — RETRY

Implement:

* controlled retry
* backoff
* configurable policy

---

## PHASE 13 — OBSERVABILITY

Implement:

* structured logs
* request IDs
* metrics
* service health
* operational events

---

## PHASE 14 — DEMO SERVICES

Create:

* user-service
* product-service
* order-service

---

## PHASE 15 — COMPLETE INTEGRATION

Connect all components.

---

## PHASE 16 — TESTING

Run:

* unit
* integration
* security
* concurrency
* failure/recovery tests

---

## PHASE 17 — LOAD TESTING

Run real benchmarks.

---

## PHASE 18 — DOCKER

Make the environment reproducible.

---

## PHASE 19 — DOCUMENTATION

Document the system based on actual implementation.

---

## PHASE 20 — P1/P2

Only after all P0 requirements work.

---

# 62. DEVELOPMENT GATES

Do not move forward while the current major capability is broken.

## GATE 1

Backend builds and starts.

## GATE 2

Registration works.

## GATE 3

Login works.

## GATE 4

Change password works.

## GATE 5

Forgot/reset password works.

## GATE 6

Role authorization works.

## GATE 7

Admin Panel works.

## GATE 8

User Panel works.

## GATE 9

Gateway proxies requests.

## GATE 10

Service discovery works.

## GATE 11

Load balancing works.

## GATE 12

Rate limiting works.

## GATE 13

Circuit breaker works.

## GATE 14

Retry works.

## GATE 15

Metrics/logging works.

## GATE 16

Automated tests pass.

## GATE 17

Complete system runs cleanly from a fresh local start.

---

# 63. FINAL END-TO-END ACCEPTANCE TEST

Actually execute the following.

## AUTHENTICATION

1. Register user.
2. Verify secure password hash is stored.
3. Login.
4. Verify authenticated request works.
5. Logout.
6. Verify authentication state is invalidated appropriately.

## PASSWORD MANAGEMENT

7. Change password.
8. Verify old password no longer works.
9. Start forgot-password flow.
10. Generate reset token.
11. Reset password.
12. Verify old password no longer works.
13. Verify new password works.
14. Reuse old reset token.
15. Verify it is rejected.

## AUTHORIZATION

16. Login as USER.
17. Call admin endpoint.
18. Verify access denied.
19. Login as ADMIN.
20. Verify admin endpoint works.

## ADMIN USER MANAGEMENT

21. Open user list.
22. Verify user information is displayed.
23. Verify password is NOT displayed.
24. Force password reset.
25. Verify the user must establish a new password.

## GATEWAY

26. Configure a route.
27. Send request through gateway.
28. Verify downstream service response.
29. Verify request ID.

## LOAD BALANCING

30. Run multiple service instances.
31. Send multiple requests.
32. Verify requests are distributed.

## HEALTH

33. Stop one service instance.
34. Verify health check detects failure.
35. Verify traffic avoids failed instance.
36. Restart instance.
37. Verify recovery.

## RATE LIMIT

38. Configure limit.
39. Send requests within limit.
40. Exceed limit.
41. Verify HTTP 429.

## RETRY

42. Introduce temporary backend failure.
43. Verify retry occurs.
44. Verify successful recovery when applicable.

## CIRCUIT BREAKER

45. Cause repeated backend failures.
46. Verify OPEN state.
47. Verify normal traffic stops reaching failed backend.
48. Restore backend.
49. Verify HALF_OPEN.
50. Verify CLOSED after successful recovery.

## USER DATA ISOLATION

51. Create User A.
52. Create User B.
53. Generate API usage for both.
54. Verify User A cannot see User B's private information.
55. Verify User A cannot manage User B's API keys.

## OBSERVABILITY

56. Verify request logs.
57. Verify metrics.
58. Verify request IDs.
59. Verify service status.
60. Verify admin dashboard reflects actual activity.
61. Verify user dashboard reflects only that user's activity.

## FINAL

62. Run full test suite.
63. Run load tests.
64. Verify build succeeds.
65. Start complete environment from a clean state.
66. Verify Admin Panel.
67. Verify User Panel.
68. Verify P0 functionality.

Create:

`docs/mvp-validation.md`

with actual results.

---

# 64. NO FAKE COMPLETION

Never say:

* "implemented" when code was only scaffolded
* "tested" when tests were not run
* "working" when the feature was not verified
* "production-ready" without qualification
* "supports X requests/sec" without benchmarks
* "AWS deployed" when not deployed

When something cannot be completed because of the environment, document:

* what failed
* why it failed
* what was completed
* what remains

Then continue with everything else that can genuinely be completed.

---

# 65. FINAL QUALITY BAR

The final SentinelX project should feel like a serious industry-style engineering project.

It should demonstrate:

```text
Spring Boot
+
REST APIs
+
HTTP/networking
+
Authentication
+
Authorization
+
Password recovery
+
API keys
+
API Gateway
+
Dynamic routing
+
Service discovery
+
Load balancing
+
Rate limiting
+
Concurrency
+
Circuit breakers
+
Retries
+
Health checks
+
Database design
+
Caching
+
Observability
+
Structured logging
+
Testing
+
Load testing
+
Docker
+
Professional Admin UI
+
Professional User/Developer UI
+
AWS-ready architecture
```

Most importantly:

Do not optimize for the number of files or technologies.

Optimize for:

**A coherent system where every important component actually works together.**

Start by inspecting:

`D:\projects\sentineIX`

and especially:

`D:\projects\sentineIX\backend`

Then implement the project incrementally according to the P0 priorities and development gates.

Do not initialize Git.

Do not create a GitHub repository.

Do not store plaintext passwords.

Do not expose passwords or password hashes through the application UI.

Create only the `.gitignore` for source-control hygiene.
