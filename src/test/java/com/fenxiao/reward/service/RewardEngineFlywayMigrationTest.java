package com.fenxiao.reward.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class RewardEngineFlywayMigrationTest {

    @Test
    void shouldApplyRewardEngineAuditMigration() throws Exception {
        String databaseUrl = "jdbc:h2:mem:reward_engine_flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(databaseUrl, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("create table income_event (id bigint primary key, event_time timestamp not null)");
            statement.execute("create table reward_record (id bigint primary key, reward_level int not null, calculated_at timestamp not null)");
            statement.execute("insert into reward_record (id, reward_level, calculated_at) values (1, 1, current_timestamp), (2, 2, current_timestamp)");
        }
        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("14")
                .target("15")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (var connection = DriverManager.getConnection(databaseUrl, "sa", "");
             var statement = connection.createStatement()) {
            assertThat(statement.executeQuery("select reward_engine_version, reward_processing_status, reward_decision_json from income_event").getMetaData().getColumnCount())
                    .isEqualTo(3);
            try (var rewardRows = statement.executeQuery("select reward_level, reward_engine_version, reward_type from reward_record order by id")) {
                assertThat(rewardRows.next()).isTrue();
                assertThat(rewardRows.getInt("reward_level")).isEqualTo(1);
                assertThat(rewardRows.getString("reward_engine_version")).isEqualTo("LEGACY_V0");
                assertThat(rewardRows.getString("reward_type")).isEqualTo("DIRECT_RECRUIT");
                assertThat(rewardRows.next()).isTrue();
                assertThat(rewardRows.getInt("reward_level")).isEqualTo(2);
                assertThat(rewardRows.getString("reward_type")).isEqualTo("LEGACY_LEVEL");
            }
        }
    }
}
