ALTER TABLE user_distribution_profile
    ADD COLUMN api_access_token VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_user_distribution_profile_api_access_token
    ON user_distribution_profile (api_access_token);
