CREATE TABLE invite_code_issue_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    issuer_user_id BIGINT NOT NULL UNIQUE,
    product_code VARCHAR(32) NOT NULL,
    whatsapp_number VARCHAR(32) NOT NULL UNIQUE,
    app_account VARCHAR(16) NOT NULL UNIQUE,
    invite_code VARCHAR(20) NOT NULL,
    issued_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_invite_code_issue_product (product_code),
    INDEX idx_invite_code_issue_invite_code (invite_code)
);