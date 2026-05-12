CREATE TABLE linky_account_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    linky_account VARCHAR(32) NOT NULL UNIQUE,
    phone_number VARCHAR(32) NULL,
    invite_code VARCHAR(20) NULL,
    guild_id VARCHAR(64) NULL,
    guild_name VARCHAR(128) NULL,
    guild_owner_group VARCHAR(64) NULL,
    guild_check_status VARCHAR(32) NOT NULL,
    registration_eligibility VARCHAR(32) NOT NULL,
    checked_at DATETIME NULL,
    checked_by BIGINT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_linky_account_binding_user (user_id),
    INDEX idx_linky_account_binding_eligibility (registration_eligibility, guild_check_status)
);
