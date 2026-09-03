package com.fenxiao.identity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityFoundationFlywayMigrationTest {
    @Test
    void shouldApplyV16AndBackfillIndependentRelationships() throws Exception {
        String url = "jdbc:h2:mem:identity_v16;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table user_distribution_profile (user_id bigint primary key, country_code varchar(10) not null, registered_at timestamp not null)");
            statement.execute("create table distribution_relation (id bigint primary key, user_id bigint not null, level1_inviter_id bigint, bind_time timestamp not null)");
            statement.execute("insert into user_distribution_profile values (1,'BR',current_timestamp),(2,'BR',current_timestamp)");
            statement.execute("insert into distribution_relation values (1,1,null,current_timestamp),(2,2,1,current_timestamp)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("15").target("16").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            assertThat(statement.executeQuery("select count(*) from invitation_relation_version").next()).isTrue();
            try (var row = statement.executeQuery("select count(*) from invitation_relation_version")) { row.next(); assertThat(row.getInt(1)).isEqualTo(2); }
            try (var row = statement.executeQuery("select count(*) from mentor_assignment_version where assignment_status='MENTOR_ASSIGNMENT_PENDING'")) { row.next(); assertThat(row.getInt(1)).isEqualTo(2); }
            try (var row = statement.executeQuery("select count(*) from team_membership_version")) { row.next(); assertThat(row.getInt(1)).isEqualTo(2); }
            try (var row = statement.executeQuery("select account_status from user_distribution_profile where user_id=1")) { row.next(); assertThat(row.getString(1)).isEqualTo("ACTIVE"); }
        }
    }
}
