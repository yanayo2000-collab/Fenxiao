CREATE TABLE user_distribution_profile (
    user_id BIGINT PRIMARY KEY,
    country_code VARCHAR(10) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    invite_code VARCHAR(20) NOT NULL UNIQUE,
    distribution_role VARCHAR(32) NOT NULL,
    user_status VARCHAR(32) NOT NULL,
    is_effective_user BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_income_total DECIMAL(18,6) NOT NULL DEFAULT 0,
    registered_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE distribution_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    level1_inviter_id BIGINT NULL,
    level2_inviter_id BIGINT NULL,
    level3_inviter_id BIGINT NULL,
    bind_source VARCHAR(32) NOT NULL,
    bind_time DATETIME NOT NULL,
    lock_status VARCHAR(32) NOT NULL,
    lock_time DATETIME NULL,
    country_code VARCHAR(10) NOT NULL,
    cross_country BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_distribution_relation_level1 (level1_inviter_id),
    INDEX idx_distribution_relation_level2 (level2_inviter_id),
    INDEX idx_distribution_relation_level3 (level3_inviter_id)
);

CREATE TABLE income_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_event_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    income_type VARCHAR(32) NOT NULL,
    income_amount DECIMAL(18,6) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    event_time DATETIME NOT NULL,
    sync_batch_no VARCHAR(64) NULL,
    sync_status VARCHAR(32) NOT NULL,
    raw_payload LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE reward_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    country_code VARCHAR(10) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    reward_level INT NOT NULL,
    reward_rate DECIMAL(8,6) NOT NULL,
    freeze_days INT NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_reward_rule_lookup (country_code, role_code, reward_level, status)
);

CREATE TABLE reward_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_event_id VARCHAR(64) NOT NULL,
    beneficiary_user_id BIGINT NOT NULL,
    source_user_id BIGINT NOT NULL,
    reward_level INT NOT NULL,
    income_amount DECIMAL(18,6) NOT NULL,
    reward_rate DECIMAL(8,6) NOT NULL,
    reward_amount DECIMAL(18,6) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    reward_status VARCHAR(32) NOT NULL,
    unfreeze_at DATETIME NULL,
    calculated_at DATETIME NOT NULL,
    settled_at DATETIME NULL,
    risk_flag BOOLEAN NOT NULL DEFAULT FALSE,
    risk_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_reward_record_idempotent (source_event_id, beneficiary_user_id, reward_level),
    INDEX idx_reward_record_beneficiary_status (beneficiary_user_id, reward_status),
    INDEX idx_reward_record_source_user (source_user_id)
);

CREATE TABLE risk_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    risk_type VARCHAR(64) NOT NULL,
    risk_level INT NOT NULL,
    risk_status VARCHAR(32) NOT NULL,
    detail_json LONGTEXT NULL,
    detected_at DATETIME NOT NULL,
    handled_by BIGINT NULL,
    handled_at DATETIME NULL,
    result_note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_risk_event_user_status (user_id, risk_status)
);

CREATE TABLE operation_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    module_name VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    before_data LONGTEXT NULL,
    after_data LONGTEXT NULL,
    request_ip VARCHAR(64) NULL,
    operated_at DATETIME NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_audit_module_operated_at (module_name, operated_at)
);
