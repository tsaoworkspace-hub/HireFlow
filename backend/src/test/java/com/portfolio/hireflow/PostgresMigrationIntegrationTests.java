package com.portfolio.hireflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.mail.enabled=false",
        "app.google-calendar.enabled=false",
        "app.demo-accounts.enabled=false"
})
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named = "HIREFLOW_POSTGRES_TEST", matches = "true")
class PostgresMigrationIntegrationTests {

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationsAndHibernateValidationSucceedOnPostgres() {
        assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version is not null",
                Integer.class)).isGreaterThanOrEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from calendar_sync_jobs", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from candidate_attachments", Long.class)).isZero();
    }
}
