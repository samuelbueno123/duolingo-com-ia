-- Flyway V3: Audit Logs and Administrative Management
-- Creates audit_logs table and adds active status to users for account suspension/reactivation

-- 1. Ensure active column exists in users
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor_email VARCHAR(255),
    actor_role VARCHAR(50),
    target_entity VARCHAR(100),
    target_id VARCHAR(100),
    result VARCHAR(30) NOT NULL,
    client_ip VARCHAR(60),
    correlation_id VARCHAR(100),
    details TEXT
);

-- 3. High-performance indexes for audit log filtering, sorting, and reporting
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_category ON audit_logs (category);
CREATE INDEX IF NOT EXISTS idx_audit_logs_severity ON audit_logs (severity);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_logs_result ON audit_logs (result);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
