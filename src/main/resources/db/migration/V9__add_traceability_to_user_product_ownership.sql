ALTER TABLE user_product_ownership
    ADD COLUMN source_record_type VARCHAR(64) NULL AFTER ownership_source,
    ADD COLUMN source_record_id BIGINT NULL AFTER source_record_type;

UPDATE user_product_ownership
SET source_record_type = 'LEGACY_MIGRATED',
    source_record_id = NULL
WHERE source_record_type IS NULL;