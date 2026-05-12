ALTER TABLE reward_record
    ADD COLUMN withdraw_status VARCHAR(32) NOT NULL DEFAULT 'UNCLAIMED';

CREATE TABLE withdraw_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    requested_diamond_amount DECIMAL(18,6) NOT NULL,
    approved_diamond_amount DECIMAL(18,6) NULL,
    request_status VARCHAR(32) NOT NULL,
    request_week VARCHAR(20) NOT NULL,
    requested_at DATETIME NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    paid_at DATETIME NULL,
    reject_reason VARCHAR(255) NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_withdraw_request_user_week (user_id, request_week),
    INDEX idx_withdraw_request_status (request_status)
);

CREATE TABLE withdraw_request_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdraw_request_id BIGINT NOT NULL,
    reward_record_id BIGINT NOT NULL,
    reward_amount DECIMAL(18,6) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_withdraw_request_item (withdraw_request_id, reward_record_id),
    INDEX idx_withdraw_request_item_reward (reward_record_id)
);
