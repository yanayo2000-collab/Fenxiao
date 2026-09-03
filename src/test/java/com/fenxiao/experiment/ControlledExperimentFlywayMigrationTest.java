package com.fenxiao.experiment;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class ControlledExperimentFlywayMigrationTest {
    @Test
    void shouldApplyV20ExperimentSchema() throws Exception {
        String url = "jdbc:h2:mem:experiment_v20;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table withdraw_request (id bigint primary key)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("19").target("20").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('controlled_experiment','experiment_participant','experiment_metric_event','experiment_status_transition')")) {
            result.next(); assertThat(result.getInt(1)).isEqualTo(4);
        }
    }
}
