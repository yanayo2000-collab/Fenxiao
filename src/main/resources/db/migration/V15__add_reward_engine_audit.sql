ALTER TABLE income_event
    ADD COLUMN reward_engine_version VARCHAR(32) NOT NULL DEFAULT 'LEGACY_V0';

ALTER TABLE income_event
    ADD COLUMN legacy_candidate_count INT NOT NULL DEFAULT 0;

ALTER TABLE income_event
    ADD COLUMN generated_reward_count INT NOT NULL DEFAULT 0;

ALTER TABLE income_event
    ADD COLUMN reward_processing_status VARCHAR(32) NOT NULL DEFAULT 'LEGACY_PROCESSED';

ALTER TABLE income_event
    ADD COLUMN reward_decision_json LONGTEXT NULL;

CREATE INDEX idx_income_event_reward_engine
    ON income_event (reward_engine_version, event_time);

ALTER TABLE reward_record
    ADD COLUMN reward_engine_version VARCHAR(32) NOT NULL DEFAULT 'LEGACY_V0';

ALTER TABLE reward_record
    ADD COLUMN reward_type VARCHAR(32) NOT NULL DEFAULT 'LEGACY_LEVEL';

UPDATE reward_record
SET reward_type = 'DIRECT_RECRUIT'
WHERE reward_level = 1;

CREATE INDEX idx_reward_record_engine_type
    ON reward_record (reward_engine_version, reward_type, calculated_at);
