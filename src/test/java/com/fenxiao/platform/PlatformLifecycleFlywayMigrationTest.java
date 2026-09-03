package com.fenxiao.platform;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformLifecycleFlywayMigrationTest {
    @Test
    void shouldApplyV17PlatformAndLifecycleSchema() throws Exception {
        String url = "jdbc:h2:mem:platform_v17;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table existing_v16_marker (id bigint primary key)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("16").target("17").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('platform_account_binding','platform_binding_history','platform_business_fact','platform_milestone_policy','platform_lifecycle_snapshot')")) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(5);
        }
    }
}
