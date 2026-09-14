CREATE TABLE users (
 id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN','OPERATOR','DEVELOPER','USER')),
 active BOOLEAN NOT NULL DEFAULT TRUE, api_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 reset_required BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, last_login TIMESTAMP
);
CREATE TABLE sessions (
 token_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL REFERENCES users(id),
 created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE TABLE password_reset_tokens (
 token_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL REFERENCES users(id),
 created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP
);
CREATE INDEX idx_reset_user ON password_reset_tokens(user_id);
CREATE TABLE api_keys (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL REFERENCES users(id), name VARCHAR(100) NOT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE, prefix VARCHAR(16) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 revoked BOOLEAN NOT NULL DEFAULT FALSE, tier VARCHAR(20) NOT NULL DEFAULT 'STANDARD', created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_keys_user ON api_keys(user_id);
CREATE TABLE services (
 id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 strategy VARCHAR(30) NOT NULL CHECK(strategy IN ('ROUND_ROBIN','LEAST_CONNECTIONS')), created_at TIMESTAMP NOT NULL
);
CREATE TABLE service_instances (
 id VARCHAR(36) PRIMARY KEY, service_id VARCHAR(36) NOT NULL REFERENCES services(id), base_url VARCHAR(500) NOT NULL,
 health_path VARCHAR(100) NOT NULL DEFAULT '/health', enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL,
 UNIQUE(service_id,base_url)
);
CREATE TABLE routes (
 id VARCHAR(36) PRIMARY KEY, path_prefix VARCHAR(200) NOT NULL UNIQUE, methods VARCHAR(100) NOT NULL,
 service_id VARCHAR(36) NOT NULL REFERENCES services(id), enabled BOOLEAN NOT NULL DEFAULT TRUE,
 timeout_ms INTEGER NOT NULL CHECK(timeout_ms BETWEEN 100 AND 30000), retries INTEGER NOT NULL CHECK(retries BETWEEN 0 AND 3),
 backoff_ms INTEGER NOT NULL CHECK(backoff_ms BETWEEN 0 AND 1000), failure_threshold INTEGER NOT NULL CHECK(failure_threshold BETWEEN 1 AND 100),
 recovery_ms INTEGER NOT NULL CHECK(recovery_ms BETWEEN 100 AND 300000), created_at TIMESTAMP NOT NULL
);
CREATE TABLE rate_limit_policies (
 id VARCHAR(36) PRIMARY KEY, scope VARCHAR(20) NOT NULL CHECK(scope IN ('GLOBAL','IP','USER','KEY','ROUTE')),
 capacity INTEGER NOT NULL CHECK(capacity BETWEEN 1 AND 100000), refill_per_second DOUBLE PRECISION NOT NULL CHECK(refill_per_second > 0),
 enabled BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE(scope)
);
CREATE TABLE request_logs (
 id VARCHAR(36) PRIMARY KEY, request_id VARCHAR(64) NOT NULL, user_id VARCHAR(36) REFERENCES users(id), key_id VARCHAR(36),
 method VARCHAR(10) NOT NULL, path VARCHAR(500) NOT NULL, service_id VARCHAR(36), instance_id VARCHAR(36),
 status INTEGER NOT NULL, latency_ms DOUBLE PRECISION NOT NULL, retries INTEGER NOT NULL, created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_requests_user_time ON request_logs(user_id,created_at);
CREATE INDEX idx_requests_time ON request_logs(created_at);
CREATE TABLE audit_logs (
 id VARCHAR(36) PRIMARY KEY, actor VARCHAR(36), action VARCHAR(100) NOT NULL, resource VARCHAR(200) NOT NULL,
 request_id VARCHAR(64), created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_time ON audit_logs(created_at);
