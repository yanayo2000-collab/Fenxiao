CREATE TABLE platform_account_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    platform_user_id VARCHAR(64) NOT NULL,
    binding_status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    official_guild_id VARCHAR(64),
    official_joined_at TIMESTAMP,
    verification_source VARCHAR(64),
    verification_reference VARCHAR(128),
    rejection_code VARCHAR(64),
    rejection_reason VARCHAR(255),
    version_no INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_platform_binding_platform_id ON platform_account_binding(platform_code, platform_user_id);
CREATE UNIQUE INDEX uk_platform_binding_user_platform ON platform_account_binding(user_id, platform_code);
CREATE INDEX idx_platform_binding_status ON platform_account_binding(binding_status, platform_code);

CREATE TABLE platform_binding_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    binding_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    platform_user_id VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64),
    reason_detail VARCHAR(255),
    source_system VARCHAR(64) NOT NULL,
    operator_id BIGINT,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_platform_binding_history_binding ON platform_binding_history(binding_id, occurred_at);

CREATE TABLE platform_business_fact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_event_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    platform_user_id VARCHAR(64) NOT NULL,
    fact_type VARCHAR(64) NOT NULL,
    amount DECIMAL(18,6),
    currency_code VARCHAR(16),
    occurred_at TIMESTAMP NOT NULL,
    business_date DATE NOT NULL,
    guild_id VARCHAR(64),
    source_system VARCHAR(64) NOT NULL,
    source_version VARCHAR(64),
    payload_hash VARCHAR(128),
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_platform_business_fact_source ON platform_business_fact(source_system, source_event_id);
CREATE INDEX idx_platform_business_fact_lifecycle ON platform_business_fact(user_id, platform_code, business_date, fact_type);

CREATE TABLE platform_milestone_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_code VARCHAR(32) NOT NULL,
    guild_id VARCHAR(64) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    minimum_withdrawable_amount DECIMAL(18,6) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_platform_milestone_policy_scope ON platform_milestone_policy(platform_code, guild_id, country_code, enabled);

CREATE TABLE platform_lifecycle_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    platform_user_id VARCHAR(64) NOT NULL,
    lifecycle_start_date DATE,
    binding_verified BOOLEAN NOT NULL DEFAULT FALSE,
    first_income_at TIMESTAMP,
    first_withdraw_eligible_at TIMESTAMP,
    consecutive_7_day_active BOOLEAN NOT NULL DEFAULT FALSE,
    consecutive_30_day_active BOOLEAN NOT NULL DEFAULT FALSE,
    consecutive_active_days INT NOT NULL DEFAULT 0,
    cumulative_net_income DECIMAL(18,6) NOT NULL DEFAULT 0,
    shadow_only BOOLEAN NOT NULL DEFAULT TRUE,
    evaluated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_platform_lifecycle_user_platform ON platform_lifecycle_snapshot(user_id, platform_code);

