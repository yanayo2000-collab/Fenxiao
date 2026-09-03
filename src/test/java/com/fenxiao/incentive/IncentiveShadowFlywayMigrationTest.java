package com.fenxiao.incentive;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class IncentiveShadowFlywayMigrationTest {
    @Test
    void shouldApplyV18ShadowLedgerSchema() throws Exception {
        String url = "jdbc:h2:mem:incentive_v18;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table platform_lifecycle_snapshot (id bigint primary key)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("17").target("18").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('incentive_rule_version','incentive_shadow_ledger','leadership_policy_version','leadership_qualification','team_profit_fact','team_profit_share_shadow_ledger')")) {
            result.next(); assertThat(result.getInt(1)).isEqualTo(6);
        }
    }
}
