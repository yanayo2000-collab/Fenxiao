CREATE TABLE controlled_experiment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_code VARCHAR(64) NOT NULL,
    experiment_name VARCHAR(128) NOT NULL,
    experiment_status VARCHAR(32) NOT NULL,
    planned_sample_size INT NOT NULL,
    primary_metric_code VARCHAR(64) NOT NULL,
    enrollment_starts_at TIMESTAMP NOT NULL,
    enrollment_ends_at TIMESTAMP NOT NULL,
    observation_ends_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_controlled_experiment_code (experiment_code)
);

CREATE TABLE experiment_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    enrollment_no INT NOT NULL,
    user_id BIGINT NOT NULL,
    participant_status VARCHAR(32) NOT NULL,
    cohort_code VARCHAR(32) NOT NULL,
    eligibility_snapshot VARCHAR(2000) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    activated_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_experiment_participant_user (experiment_id, user_id),
    UNIQUE KEY uk_experiment_enrollment_no (experiment_id, enrollment_no),
    INDEX idx_experiment_participant_status (experiment_id, participant_status)
);

CREATE TABLE experiment_metric_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    metric_value DECIMAL(18,6) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    source_event_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_experiment_metric_source (source_system, source_event_id),
    INDEX idx_experiment_metric_rollup (experiment_id, metric_code, occurred_at)
);

CREATE TABLE experiment_status_transition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    participant_id BIGINT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id BIGINT NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_experiment_transition (experiment_id, occurred_at)
);
