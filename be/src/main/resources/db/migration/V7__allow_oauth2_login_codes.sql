ALTER TABLE account_action_tokens
    DROP CONSTRAINT ck_account_action_tokens_purpose;

ALTER TABLE account_action_tokens
    ADD CONSTRAINT ck_account_action_tokens_purpose
        CHECK (purpose IN ('PASSWORD_RESET', 'EMAIL_CHANGE', 'OAUTH2_LOGIN'));
