ALTER TABLE withdraw_request ADD COLUMN payment_channel VARCHAR(32) NULL;
ALTER TABLE withdraw_request ADD COLUMN payment_reference VARCHAR(128) NULL;
ALTER TABLE withdraw_request ADD COLUMN payment_evidence_uri VARCHAR(255) NULL;
ALTER TABLE withdraw_request ADD COLUMN payment_evidence_hash VARCHAR(128) NULL;
ALTER TABLE withdraw_request ADD COLUMN paid_by BIGINT NULL;
ALTER TABLE withdraw_request ADD COLUMN payment_failure_reason VARCHAR(255) NULL;
ALTER TABLE withdraw_request ADD COLUMN reversed_at DATETIME NULL;
ALTER TABLE withdraw_request ADD COLUMN reversed_by BIGINT NULL;
ALTER TABLE withdraw_request ADD COLUMN reversal_reason VARCHAR(255) NULL;

CREATE TABLE withdraw_request_transition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdraw_request_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id BIGINT NULL,
    actor_role VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_withdraw_transition_request (withdraw_request_id, occurred_at)
);

CREATE TABLE withdraw_payment_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdraw_request_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    payment_channel VARCHAR(32) NOT NULL,
    payment_reference VARCHAR(128) NULL,
    evidence_uri VARCHAR(255) NULL,
    evidence_hash VARCHAR(128) NULL,
    attempt_status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    operated_by BIGINT NOT NULL,
    operated_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_withdraw_payment_attempt (withdraw_request_id, attempt_no),
    INDEX idx_withdraw_payment_reference (payment_channel, payment_reference)
);

CREATE TABLE withdraw_reconciliation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdraw_request_id BIGINT NOT NULL,
    reconciliation_status VARCHAR(32) NOT NULL,
    external_reference VARCHAR(128) NULL,
    external_amount DECIMAL(18,6) NULL,
    currency_code VARCHAR(16) NULL,
    details VARCHAR(500) NULL,
    reconciled_by BIGINT NOT NULL,
    reconciled_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_withdraw_reconciliation_request (withdraw_request_id, reconciled_at)
);

CREATE TABLE withdraw_reversal_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdraw_request_id BIGINT NOT NULL,
    reversal_amount DECIMAL(18,6) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reversed_by BIGINT NOT NULL,
    reversed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_withdraw_reversal_request (withdraw_request_id)
);
