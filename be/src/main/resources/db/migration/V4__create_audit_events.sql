CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    actor_type VARCHAR(16) NOT NULL,
    action VARCHAR(96) NOT NULL,
    resource_type VARCHAR(96) NOT NULL,
    resource_id UUID,
    reason VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    request_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_events_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_audit_events_actor_type
        CHECK (actor_type IN ('USER', 'ADMIN', 'SYSTEM')),
    CONSTRAINT ck_audit_events_system_actor
        CHECK (actor_type = 'SYSTEM' OR actor_user_id IS NOT NULL)
);

CREATE INDEX idx_audit_events_actor_created_at
    ON audit_events(actor_user_id, created_at DESC);

CREATE INDEX idx_audit_events_resource_created_at
    ON audit_events(resource_type, resource_id, created_at DESC);

CREATE INDEX idx_audit_events_action_created_at
    ON audit_events(action, created_at DESC);
