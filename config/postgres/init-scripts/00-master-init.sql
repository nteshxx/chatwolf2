-- Master initialization script for all databases
-- This script sets up users, databases, and extensions

-- Create extensions in template1 (available to all databases)
\c postgres
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create application users (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'chatwolf_user') THEN
        CREATE USER chatwolf_user WITH PASSWORD 'chatwolfstrongpassword' VALID UNTIL 'infinity';
        RAISE NOTICE 'Created user: chatwolf_user';
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'grafana_user') THEN
        CREATE USER grafana_user WITH PASSWORD 'grafanastrongpassword' VALID UNTIL 'infinity';
        RAISE NOTICE 'Created user: grafana_user';
    END IF;
END
$$;

-- Create chatwolf database
SELECT 'CREATE DATABASE chatwolf_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chatwolf_db')\gexec

-- Create grafana database
SELECT 'CREATE DATABASE grafana_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'grafana_db')\gexec

-- Grant privileges
ALTER USER chatwolf_user SET search_path = chatwolf, public;
ALTER USER grafana_user SET search_path = grafana, public;

DO $$
BEGIN
    RAISE NOTICE 'Master Initialization Completed at: %', NOW();
END $$;
