package com.fenxiao.admin.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class AdminIdentityFlywayMigrationTest {
    @Test void appliesV21WithoutChangingExistingAdminCredentials() throws Exception {
        String url="jdbc:h2:mem:admin_identity_v21;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()){
            s.execute("create table admin_account(id bigint auto_increment primary key,username varchar(64) not null,display_name varchar(128) not null,role varchar(32) not null,password_hash varchar(256) not null,enabled boolean not null,last_login_at timestamp null,created_at timestamp not null default current_timestamp,updated_at timestamp not null default current_timestamp)");
            s.execute("insert into admin_account(username,display_name,role,password_hash,enabled) values('root','Root','super_admin','existing-hash',true)");
        }
        Flyway flyway=Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("20").target("21").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()){
            try(var row=s.executeQuery("select password_hash,platform_scope,must_change_password from admin_account where username='root'")){assertThat(row.next()).isTrue();assertThat(row.getString(1)).isEqualTo("existing-hash");assertThat(row.getString(2)).isEqualTo("*");assertThat(row.getBoolean(3)).isFalse();}
            try(var tables=s.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('admin_session','admin_password_history','admin_security_event')")){tables.next();assertThat(tables.getInt(1)).isEqualTo(3);}
        }
    }
}
