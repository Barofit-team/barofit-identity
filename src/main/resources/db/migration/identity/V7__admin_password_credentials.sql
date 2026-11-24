CREATE TABLE identity.admin_credentials (
 username VARCHAR(64) PRIMARY KEY,
 user_id UUID NOT NULL UNIQUE REFERENCES identity.users(id),
 password_hash VARCHAR(100) NOT NULL,
 failed_attempts INTEGER NOT NULL DEFAULT 0,
 locked_until TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL
);
