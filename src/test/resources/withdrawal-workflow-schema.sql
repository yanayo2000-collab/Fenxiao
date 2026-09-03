CREATE TABLE IF NOT EXISTS withdraw_request_transition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, withdraw_request_id BIGINT NOT NULL, from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL, actor_id BIGINT, actor_role VARCHAR(32) NOT NULL, reason VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS withdraw_payment_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, withdraw_request_id BIGINT NOT NULL, attempt_no INT NOT NULL,
    payment_channel VARCHAR(32) NOT NULL, payment_reference VARCHAR(128), evidence_uri VARCHAR(255),
    evidence_hash VARCHAR(128), attempt_status VARCHAR(32) NOT NULL, failure_reason VARCHAR(255),
    operated_by BIGINT NOT NULL, operated_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(withdraw_request_id, attempt_no)
);
CREATE TABLE IF NOT EXISTS withdraw_reconciliation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, withdraw_request_id BIGINT NOT NULL, reconciliation_status VARCHAR(32) NOT NULL,
    external_reference VARCHAR(128), external_amount DECIMAL(18,6), currency_code VARCHAR(16), details VARCHAR(500),
    reconciled_by BIGINT NOT NULL, reconciled_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS withdraw_reversal_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, withdraw_request_id BIGINT NOT NULL, reversal_amount DECIMAL(18,6) NOT NULL,
    currency_code VARCHAR(16) NOT NULL, reason VARCHAR(255) NOT NULL, reversed_by BIGINT NOT NULL,
    reversed_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(withdraw_request_id)
);
