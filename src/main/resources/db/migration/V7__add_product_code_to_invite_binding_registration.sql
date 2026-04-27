ALTER TABLE invite_binding_registration
    ADD COLUMN product_code VARCHAR(32) NOT NULL DEFAULT 'LINKY' AFTER id;

CREATE INDEX idx_invite_binding_product
    ON invite_binding_registration (product_code);
