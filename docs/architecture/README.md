# SentinelX architecture

## Complete system

```mermaid
flowchart TB
    Admin[Operations console] --> HTTP[Spring Boot HTTP API]
    Developer[Developer portal] --> HTTP
    Client[API clients] --> Gateway[Gateway controller]
    HTTP --> Accounts[Accounts and authorization]
    HTTP --> Config[Services routes policies and consumers]
    Gateway --> Auth[Bearer session / hashed API key]
    Auth --> Limits[Token buckets]
    Limits --> Registry[Dynamic routes and registry]
    Registry --> LB[Healthy-instance load balancing]
    LB --> Circuit[Circuit and safe retry]
    Circuit --> A[User service A]
    Circuit --> B[User service B]
    Circuit --> Product[Product service]
    Circuit --> Order[Order service]
    Accounts --> DB[(PostgreSQL / local H2)]
    Config --> DB
    Gateway --> DB
    Gateway --> JSON[Structured JSON logs]
```

## Gateway request lifecycle

```mermaid
flowchart LR
    Request --> Correlation[Validate or generate request ID]
    Correlation --> Auth[Authenticate]
    Auth --> Access[Check account API access]
    Access --> Route[Resolve longest prefix and method]
    Route --> Quota[Evaluate applicable token buckets]
    Quota --> Healthy[Filter healthy and circuit-available instances]
    Healthy --> Reserve[Atomically reserve instance]
    Reserve --> Proxy[Bounded HTTP request]
    Proxy --> Release[Release reservation in finally]
    Release --> Response
    Response --> Persist[Record latency status retries and correlation]
```

## Authentication

```mermaid
sequenceDiagram
    participant Browser
    participant Auth
    participant DB
    Browser->>Auth: Email and password
    Auth->>DB: Load account password hash
    Auth->>Auth: BCrypt verify and account checks
    Auth->>DB: Persist hashed opaque session, 8-hour expiry
    Auth-->>Browser: One bearer token and safe profile
    Browser->>Auth: Bearer authenticated request
    Auth->>DB: Validate hash, expiry, state and role
    Auth-->>Browser: Authorized response
```

## Authorization

```mermaid
flowchart TD
    URL --> Public{Public endpoint?}
    Public -- No --> Session[Database-backed bearer validation]
    Session --> Role{Required role}
    Role --> Admin[ADMIN mutations and user administration]
    Role --> Ops[ADMIN or OPERATOR inspection / checks]
    Role --> Self[Authenticated own resources]
    Self --> Owner[Bind owner ID from authenticated principal]
    Public -- Gateway --> Key[API key or bearer plus API-enabled account]
```

## Password reset

```mermaid
sequenceDiagram
    participant User
    participant API
    participant DB
    participant Mailbox as Local private mailbox
    User->>API: Forgot password email
    API->>DB: Invalidate old links; save hash and expiry
    API->>Mailbox: Deliver raw reset link locally
    API-->>User: Generic response
    User->>API: Token and confirmed new password
    API->>DB: Lock unconsumed unexpired token
    API->>DB: Hash password, consume links, revoke sessions
    API-->>User: Sign in again
```

## Service discovery and health

```mermaid
flowchart LR
    Admin[Admin registration] --> DB[(Service instances)]
    DB --> Poll[Scheduled HTTP checks every 3 seconds]
    Poll --> Good[200: HEALTHY]
    Poll --> Bad[Error / timeout: UNHEALTHY]
    Good --> Eligible[Eligible for routing]
    Bad --> Exclude[Excluded immediately]
    Exclude --> Poll
    Eligible --> Stale[Exclude observations older than 15 seconds]
```

## Load balancing

```mermaid
flowchart TD
    Eligible[Eligible instance IDs] --> Strategy{Configured strategy}
    Strategy --> RR[Round robin cursor]
    Strategy --> LC[Minimum active reservation count]
    RR --> Reserve[Synchronized selection and increment]
    LC --> Reserve
    Reserve --> HTTP[Perform request outside lock]
    HTTP --> Release[Decrement in finally]
```

## Token bucket

```mermaid
flowchart LR
    Identity[GLOBAL / IP / USER / KEY / ROUTE identity] --> Refill[Refill by monotonic elapsed time]
    Refill --> Cap[Clamp to capacity]
    Cap --> Enough{At least one token?}
    Enough -- Yes --> Consume[Atomically consume one]
    Enough -- No --> Reject[429 and Retry-After]
    Consume --> Next[Next applicable policy or proxy]
```

## Circuit breaker

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN: failure threshold
    OPEN --> HALF_OPEN: cooldown elapsed and one request admitted
    HALF_OPEN --> CLOSED: successful probe
    HALF_OPEN --> OPEN: failed probe
```

Each route/instance pair has its own circuit. Old in-flight completions carry a generation and cannot overwrite a later transition. An instance with an open circuit is filtered before selection; a concurrent race may still reject a selected probe.

## Retry

```mermaid
flowchart TD
    Attempt --> Result{Result}
    Result -- Success or nonretryable --> Return
    Result -- 502 / 503 / 504 --> Safe{GET HEAD or OPTIONS?}
    Safe -- No --> Return
    Safe -- Yes --> Budget{Retries and overall time left?}
    Budget -- No --> Return
    Budget -- Yes --> Backoff[Bounded exponential backoff]
    Backoff --> Reselect[Reselect eligible instance]
    Reselect --> Attempt
```

## Admin architecture

```mermaid
flowchart LR
    Console[Operations UI] --> Bearer[Bearer and backend role checks]
    Bearer --> Users[User and consumer management]
    Bearer --> Registry[Registry route and policy management]
    Bearer --> Read[Traffic health and circuit inspection]
    Users --> Audit[(Administrative audit)]
    Registry --> Audit
    Read --> Telemetry[(Recorded telemetry)]
```

## Developer architecture

```mermaid
flowchart LR
    Portal --> Self[Profile and security]
    Portal --> Keys[Own key lifecycle]
    Portal --> Usage[Own usage and quota]
    Portal --> Explorer[Real API explorer]
    Explorer --> Gateway
    Usage --> Owner[Server-side caller ID predicate]
    Keys --> Owner
```

## Relational data model

```mermaid
erDiagram
    users ||--o{ sessions : authenticates
    users ||--o{ password_reset_tokens : recovers
    users ||--o{ api_keys : owns
    users ||--o{ request_logs : generates
    services ||--o{ service_instances : contains
    services ||--o{ routes : targets
    users {
      varchar id PK
      varchar email UK
      varchar password_hash
      varchar role
      boolean active
      boolean api_enabled
      boolean reset_required
    }
    api_keys {
      varchar id PK
      varchar user_id FK
      varchar token_hash UK
      timestamp expires_at
      boolean revoked
    }
    sessions {
      varchar token_hash PK
      varchar user_id FK
      timestamp expires_at
    }
    password_reset_tokens {
      varchar token_hash PK
      varchar user_id FK
      timestamp expires_at
      timestamp used_at
    }
    rate_limit_policies {
      varchar id PK
      varchar scope UK
      int capacity
      double refill_per_second
    }
    request_logs {
      varchar id PK
      varchar user_id FK
      varchar request_id
      int status
      double latency_ms
    }
    audit_logs {
      varchar id PK
      varchar actor
      varchar action
      varchar resource
    }
```

Roles are a constrained scalar because each account has one role. Retry and circuit policy fields belong to a route; separate tables would not add value without policy reuse. Key IDs in retained logs intentionally are not foreign keys, allowing lifecycle metadata to remain independent. Audit actor IDs are retained even if a future archival operation removes an account.

## Observability

```mermaid
flowchart LR
    Gateway --> Requests[(Request logs, 30-day retention)]
    Gateway --> JSON[Structured JSON stdout]
    Auth --> Events[(Authentication failure events)]
    Admin --> Audit[(Audit log)]
    Requests --> Metrics[SQL aggregates and bounded samples]
    Checks[Active checks] --> Health[Health snapshots]
    Metrics --> Panels[Live-data panels]
    Health --> Panels
```

## Docker deployment

```mermaid
flowchart TD
    Browser --> Port[127.0.0.1:8080]
    subgraph Compose private network
      Port --> Gateway[Gateway + embedded frontend]
      Gateway --> PG[(PostgreSQL 18 volume)]
      Gateway --> UA[User A]
      Gateway --> UB[User B]
      Gateway --> P[Product]
      Gateway --> O[Order]
      Gateway --> Mail[(Development mailbox volume)]
    end
```

## Future AWS

```mermaid
flowchart LR
    TLS[Public TLS ALB] --> ECS[Private gateway tasks]
    ECS --> Apps[Private application tasks]
    ECS --> RDS[(RDS PostgreSQL)]
    ECS -. deferred .-> Redis[(Shared traffic state)]
    ECS --> CW[CloudWatch]
```

This final diagram is a future design, not an executed deployment. See the AWS runbook for the shared-state prerequisite before scaling gateway instances.
