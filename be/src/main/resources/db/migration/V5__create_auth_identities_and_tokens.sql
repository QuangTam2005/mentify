ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN password_changed_at TIMESTAMPTZ,
    ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(320) NOT NULL,
    provider_email_verified BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_user_identities_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);

CREATE TABLE account_action_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    requested_email VARCHAR(320),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_account_action_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_account_action_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_account_action_tokens_purpose CHECK (purpose IN ('PASSWORD_RESET', 'EMAIL_CHANGE'))
);

CREATE INDEX idx_account_action_tokens_user_purpose_created
    ON account_action_tokens(user_id, purpose, created_at DESC);
