package com.fenxiao.distribution.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawalWorkflowFlywayMigrationTest {
    @Test
    void shouldApplyV19WorkflowSchema() throws Exception {
        String url = "jdbc:h2:mem:withdraw_v19;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table withdraw_request (id bigint primary key)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("18").target("19").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('withdraw_request_transition','withdraw_payment_attempt','withdraw_reconciliation_record','withdraw_reversal_ledger')")) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(4);
        }
    }
}
