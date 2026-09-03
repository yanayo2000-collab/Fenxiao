ALTER TABLE user_distribution_profile ADD COLUMN account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE user_distribution_profile ADD COLUMN cancelled_at DATETIME NULL;
ALTER TABLE user_distribution_profile ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_user_profile_account_status ON user_distribution_profile(account_status);

CREATE TABLE user_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    session_version BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    last_seen_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_session_token_hash (token_hash),
    INDEX idx_user_session_user_active (user_id, revoked_at, expires_at)
);

CREATE TABLE invitation_relation_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    inviter_user_id BIGINT NULL,
    version_no INT NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    change_reason VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(128) NULL,
    operated_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_invitation_relation_version (user_id, version_no),
    INDEX idx_invitation_relation_effective (user_id, effective_from, effective_to),
    INDEX idx_invitation_relation_inviter (inviter_user_id, effective_from)
);

CREATE TABLE mentor_profile (
    user_id BIGINT PRIMARY KEY,
    country_code VARCHAR(10) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    qualification_status VARCHAR(32) NOT NULL,
    max_active_students INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_mentor_profile_match (qualification_status, country_code, language_code)
);

CREATE TABLE mentor_assignment_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_user_id BIGINT NOT NULL,
    mentor_user_id BIGINT NULL,
    assignment_status VARCHAR(32) NOT NULL,
    version_no INT NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    change_reason VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(128) NULL,
    operated_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_mentor_assignment_version (student_user_id, version_no),
    INDEX idx_mentor_assignment_effective (student_user_id, effective_from, effective_to),
    INDEX idx_mentor_assignment_mentor_active (mentor_user_id, assignment_status, effective_to)
);

CREATE TABLE operating_team (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_code VARCHAR(64) NOT NULL,
    team_name VARCHAR(128) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    leader_user_id BIGINT NULL,
    team_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_operating_team_code (team_code),
    INDEX idx_operating_team_country_status (country_code, team_status)
);

CREATE TABLE team_membership_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    change_reason VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(128) NULL,
    operated_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_team_membership_version (user_id, version_no),
    INDEX idx_team_membership_effective (user_id, effective_from, effective_to),
    INDEX idx_team_membership_team_active (team_id, effective_to)
);

INSERT INTO invitation_relation_version (
    user_id, inviter_user_id, version_no, effective_from, effective_to,
    change_reason, source_type, source_reference, operated_by, created_at, updated_at
)
SELECT r.user_id, r.level1_inviter_id, 1, r.bind_time, NULL,
       'V16_LEGACY_BACKFILL', 'LEGACY_DISTRIBUTION_RELATION', CAST(r.id AS CHAR), NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM distribution_relation r;

INSERT INTO mentor_assignment_version (
    student_user_id, mentor_user_id, assignment_status, version_no, effective_from, effective_to,
    change_reason, source_type, source_reference, operated_by, created_at, updated_at
)
SELECT p.user_id, NULL, 'MENTOR_ASSIGNMENT_PENDING', 1, p.registered_at, NULL,
       'V16_BACKFILL_REQUIRES_OPERATIONS_ASSIGNMENT', 'MIGRATION', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_distribution_profile p;

INSERT INTO operating_team (
    team_code, team_name, country_code, leader_user_id, team_status, created_at, updated_at
)
SELECT CONCAT('SYSTEM-UNASSIGNED-', p.country_code),
       CONCAT('Awaiting team assignment (', p.country_code, ')'),
       p.country_code, NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_distribution_profile p
GROUP BY p.country_code;

INSERT INTO team_membership_version (
    user_id, team_id, version_no, effective_from, effective_to,
    change_reason, source_type, source_reference, operated_by, created_at, updated_at
)
SELECT p.user_id, t.id, 1, p.registered_at, NULL,
       'V16_BACKFILL_HOLDING_TEAM', 'MIGRATION', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_distribution_profile p
JOIN operating_team t ON t.team_code = CONCAT('SYSTEM-UNASSIGNED-', p.country_code);
