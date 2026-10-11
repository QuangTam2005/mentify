CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    user_type VARCHAR(32) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT ck_users_user_type CHECK (user_type IN ('SCHOOL_STUDENT', 'UNIVERSITY_STUDENT', 'WORKING_PROFESSIONAL')),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('ACTIVE'))
);
