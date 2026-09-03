ALTER TABLE platform_lifecycle_snapshot ADD COLUMN valid_72_hour_start BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE incentive_rule_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    rule_version INT NOT NULL,
    reward_type VARCHAR(32) NOT NULL,
    milestone_code VARCHAR(64) NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    guild_id VARCHAR(64),
    amount_minor BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    freeze_days INT NOT NULL DEFAULT 0,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_incentive_rule_version ON incentive_rule_version(rule_code, rule_version);
CREATE INDEX idx_incentive_rule_scope ON incentive_rule_version(reward_type, milestone_code, platform_code, country_code, enabled);

CREATE TABLE incentive_shadow_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(160) NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    source_user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    platform_user_id VARCHAR(64) NOT NULL,
    reward_type VARCHAR(32) NOT NULL,
    milestone_code VARCHAR(64) NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_version INT NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    ledger_status VARCHAR(32) NOT NULL,
    source_snapshot_id BIGINT,
    triggered_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_incentive_shadow_idempotency ON incentive_shadow_ledger(idempotency_key);
CREATE INDEX idx_incentive_shadow_recipient ON incentive_shadow_ledger(recipient_user_id, reward_type, ledger_status);

CREATE TABLE leadership_policy_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_code VARCHAR(64) NOT NULL,
    policy_version INT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    guild_id VARCHAR(64),
    required_valid_starts INT NOT NULL,
    required_withdraw_eligible INT NOT NULL,
    required_active_7d INT NOT NULL,
    profit_share_rate DECIMAL(8,6) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_leadership_policy_version ON leadership_policy_version(policy_code, policy_version);
CREATE INDEX idx_leadership_policy_scope ON leadership_policy_version(platform_code, country_code, enabled);

CREATE TABLE leadership_qualification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    guild_id VARCHAR(64) NOT NULL,
    qualification_code VARCHAR(64) NOT NULL,
    qualification_status VARCHAR(32) NOT NULL,
    policy_id BIGINT NOT NULL,
    valid_start_count INT NOT NULL,
    withdraw_eligible_count INT NOT NULL,
    active_7d_count INT NOT NULL,
    qualified_at TIMESTAMP,
    evaluated_at TIMESTAMP NOT NULL,
    shadow_only BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_leadership_qualification ON leadership_qualification(user_id, platform_code, guild_id, qualification_code);

CREATE TABLE team_profit_fact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_event_id VARCHAR(128) NOT NULL,
    team_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    business_income_minor BIGINT NOT NULL,
    direct_cost_minor BIGINT NOT NULL,
    recruiter_reward_minor BIGINT NOT NULL,
    mentor_reward_minor BIGINT NOT NULL,
    payment_adjustment_minor BIGINT NOT NULL,
    operating_profit_minor BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_team_profit_fact_source ON team_profit_fact(source_system, source_event_id);

CREATE TABLE team_profit_share_shadow_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(160) NOT NULL,
    team_profit_fact_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    leader_user_id BIGINT NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    policy_id BIGINT NOT NULL,
    share_rate DECIMAL(8,6) NOT NULL,
    share_amount_minor BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    ledger_status VARCHAR(32) NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_team_profit_share_shadow_key ON team_profit_share_shadow_ledger(idempotency_key);
