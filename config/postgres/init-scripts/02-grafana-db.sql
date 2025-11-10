-- Connect to grafana database
\c grafana_db

-- Create schema
CREATE SCHEMA IF NOT EXISTS grafana;
COMMENT ON SCHEMA grafana IS 'Grafana Application Schema';

-- Grant privileges
GRANT USAGE ON SCHEMA grafana TO grafana_user;
GRANT CREATE ON SCHEMA grafana TO grafana_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA grafana GRANT ALL PRIVILEGES ON TABLES TO grafana_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA grafana GRANT ALL PRIVILEGES ON SEQUENCES TO grafana_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA grafana GRANT ALL PRIVILEGES ON FUNCTIONS TO grafana_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA grafana TO grafana_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA grafana TO grafana_user;
GRANT ALL PRIVILEGES ON SCHEMA grafana TO grafana_user;

-- Session table
CREATE TABLE IF NOT EXISTS grafana.session (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    data BYTEA NOT NULL,
    expires_on INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_grafana_session_expires ON grafana.session(expires_on);

-- Performance tuning
ALTER TABLE grafana.session SET (autovacuum_vacuum_scale_factor = 0.01);
ALTER TABLE grafana.session SET (autovacuum_analyze_scale_factor = 0.005);

DO $$
BEGIN
    RAISE NOTICE 'Grafana Database Initialized at: %', NOW();
END $$;
