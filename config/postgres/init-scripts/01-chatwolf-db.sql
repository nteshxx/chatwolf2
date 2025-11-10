-- Connect to chatwolf_db
\c chatwolf_db

-- Create schema
CREATE SCHEMA IF NOT EXISTS chatwolf;
COMMENT ON SCHEMA chatwolf IS 'ChatWolf Application Schema';

-- Grant privileges to chatwolf_user
GRANT USAGE ON SCHEMA chatwolf TO chatwolf_user;
GRANT CREATE ON SCHEMA chatwolf TO chatwolf_user;

-- Grant all privileges on schema
ALTER DEFAULT PRIVILEGES IN SCHEMA chatwolf GRANT ALL PRIVILEGES ON TABLES TO chatwolf_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA chatwolf GRANT ALL PRIVILEGES ON SEQUENCES TO chatwolf_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA chatwolf GRANT ALL PRIVILEGES ON FUNCTIONS TO chatwolf_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA chatwolf TO chatwolf_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chatwolf TO chatwolf_user;
GRANT ALL PRIVILEGES ON SCHEMA chatwolf TO chatwolf_user;

DO $$
BEGIN
    RAISE NOTICE 'ChatWolf Database Initialized at: %', NOW();
END $$;
