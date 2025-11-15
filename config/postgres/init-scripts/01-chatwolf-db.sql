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

-- Create sequence management function
CREATE TABLE IF NOT EXISTS conversation_sequences (
    conversation_id VARCHAR(100) PRIMARY KEY,
    current_seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_conversation_sequences_updated 
ON conversation_sequences(updated_at);

-- Function to atomically get next sequence number
CREATE OR REPLACE FUNCTION get_next_conversation_seq(p_conversation_id VARCHAR)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_next_seq BIGINT;
BEGIN
    -- Insert new row if doesn't exist (ON CONFLICT DO NOTHING prevents race condition)
    INSERT INTO conversation_sequences (conversation_id, current_seq)
    VALUES (p_conversation_id, 1)
    ON CONFLICT (conversation_id) DO NOTHING;
    
    -- Atomically increment and return new value
    UPDATE conversation_sequences
    SET current_seq = current_seq + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE conversation_id = p_conversation_id
    RETURNING current_seq INTO v_next_seq;
    
    RETURN v_next_seq;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_next_conversation_seq(VARCHAR) TO chatwolf_user;

DO $$
BEGIN
    RAISE NOTICE 'ChatWolf Database Initialized at: %', NOW();
END $$;
