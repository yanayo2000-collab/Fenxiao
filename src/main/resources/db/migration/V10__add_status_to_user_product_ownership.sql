ALTER TABLE user_product_ownership
    ADD COLUMN ownership_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' AFTER product_code;

UPDATE user_product_ownership
SET ownership_status = 'ACTIVE'
WHERE ownership_status IS NULL;