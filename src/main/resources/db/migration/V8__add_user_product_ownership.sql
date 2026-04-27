CREATE TABLE user_product_ownership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    ownership_source VARCHAR(32) NOT NULL,
    effective_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_product_ownership_user_product (user_id, product_code),
    INDEX idx_user_product_ownership_product (product_code)
);

INSERT INTO user_product_ownership (user_id, product_code, ownership_source, effective_at, created_at, updated_at)
SELECT scoped.user_id,
       scoped.product_code,
       'LEGACY_MIGRATED',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (
    SELECT issuer_user_id AS user_id, product_code
    FROM invite_code_issue_record
    UNION
    SELECT CAST(linky_account AS BIGINT) AS user_id, product_code
    FROM invite_binding_registration
) scoped;