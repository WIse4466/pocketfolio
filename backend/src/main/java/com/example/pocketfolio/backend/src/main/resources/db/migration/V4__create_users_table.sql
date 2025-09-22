CREATE TABLE users (
    id UUID PRIMARY KEY,
    -- Add other user-related fields here later if needed, e.g., email, password_hash
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);