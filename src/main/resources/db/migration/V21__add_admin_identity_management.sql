ALTER TABLE admin_account ADD COLUMN password_changed_at TIMESTAMP NULL;
ALTER TABLE admin_account ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_account ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE admin_account ADD COLUMN last_failed_login_at TIMESTAMP NULL;
ALTER TABLE admin_account ADD COLUMN locked_until TIMESTAMP NULL;
ALTER TABLE admin_account ADD COLUMN platform_scope VARCHAR(512) NOT NULL DEFAULT '*';
ALTER TABLE admin_account ADD COLUMN guild_scope VARCHAR(1024) NOT NULL DEFAULT '*';
ALTER TABLE admin_account ADD COLUMN region_scope VARCHAR(512) NOT NULL DEFAULT '*';
ALTER TABLE admin_account ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_account ADD COLUMN mfa_secret VARCHAR(256) NULL;

CREATE TABLE admin_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    remember_me BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    revoke_reason VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_session_account FOREIGN KEY (account_id) REFERENCES admin_account(id),
    CONSTRAINT uk_admin_session_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_admin_session_account_active ON admin_session(account_id, revoked_at, expires_at);

CREATE TABLE admin_password_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_password_history_account FOREIGN KEY (account_id) REFERENCES admin_account(id)
);
CREATE INDEX idx_admin_password_history_account ON admin_password_history(account_id, created_at);

CREATE TABLE admin_security_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NULL,
    username VARCHAR(64) NULL,
    event_type VARCHAR(64) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    detail VARCHAR(512) NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_security_event_account FOREIGN KEY (account_id) REFERENCES admin_account(id)
);
CREATE INDEX idx_admin_security_event_account_time ON admin_security_event(account_id, occurred_at);
CREATE INDEX idx_admin_security_event_type_time ON admin_security_event(event_type, occurred_at);
