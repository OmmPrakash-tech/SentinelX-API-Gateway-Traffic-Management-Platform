ALTER TABLE routes ADD COLUMN connect_timeout_ms INTEGER NOT NULL DEFAULT 1000 CHECK(connect_timeout_ms BETWEEN 100 AND 10000);
ALTER TABLE routes ADD COLUMN read_timeout_ms INTEGER NOT NULL DEFAULT 2000 CHECK(read_timeout_ms BETWEEN 100 AND 30000);
CREATE TABLE system_events (
 id VARCHAR(36) PRIMARY KEY, event_type VARCHAR(50) NOT NULL, request_id VARCHAR(64), created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_system_events_time ON system_events(created_at);
