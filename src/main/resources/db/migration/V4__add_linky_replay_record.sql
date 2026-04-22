ALTER TABLE linky_webhook_log
    ADD COLUMN replay_record_status VARCHAR(32) NOT NULL DEFAULT 'NOT_RECORDED' AFTER replay_status,
    ADD COLUMN request_fingerprint VARCHAR(128) NULL AFTER replay_record_status,
    ADD COLUMN replay_hit_count INT NULL AFTER request_fingerprint;

CREATE TABLE linky_replay_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_fingerprint VARCHAR(128) NOT NULL UNIQUE,
    linky_order_id VARCHAR(80) NOT NULL,
    source_event_id VARCHAR(80) NOT NULL,
    user_id BIGINT NOT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    hit_count INT NOT NULL,
    latest_request_status VARCHAR(32) NOT NULL,
    latest_failure_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_linky_replay_order_seen (linky_order_id, last_seen_at),
    INDEX idx_linky_replay_user_seen (user_id, last_seen_at)
);
