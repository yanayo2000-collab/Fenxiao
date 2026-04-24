CREATE TABLE invite_binding_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inviter_user_id BIGINT NOT NULL,
    invite_code VARCHAR(20) NOT NULL,
    whatsapp_number VARCHAR(32) NOT NULL UNIQUE,
    linky_account VARCHAR(16) NOT NULL UNIQUE,
    bind_status VARCHAR(32) NOT NULL,
    submitted_at DATETIME NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_invite_binding_inviter (inviter_user_id),
    INDEX idx_invite_binding_invite_code (invite_code)
);
