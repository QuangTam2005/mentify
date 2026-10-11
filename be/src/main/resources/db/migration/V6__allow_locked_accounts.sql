ALTER TABLE users DROP CONSTRAINT ck_users_account_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_account_status
    CHECK (account_status IN ('ACTIVE', 'LOCKED'));
