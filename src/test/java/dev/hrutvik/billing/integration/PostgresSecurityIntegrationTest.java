package dev.hrutvik.billing.integration;

import dev.hrutvik.billing.security.ApiCredentialStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresSecurityIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("billing_test").withUsername("billing").withPassword("billing");
    private JdbcTemplate jdbc;

    @BeforeAll void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test void appliesEveryProductionMigrationToPostgres16() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.api_credentials') IS NOT NULL", Boolean.class)).isTrue();
    }

    @Test void storesScopesReadsCredentialAndAuditsUse() {
        UUID credentialId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO api_credentials(id,key_id,secret_hash,tenant_id,display_name,scopes)
                VALUES (?,?,'cd577fe2561ebff23505db0bb006300c7cdecbd46bc0e03c449afafaca2c25bf',?,?,ARRAY['usage:read','billing:write'])
                """, credentialId, "integration-key", "tenant-a", "Integration test");
        var store = new ApiCredentialStore(jdbc);
        var credential = store.find("integration-key").orElseThrow();
        assertThat(credential.tenantId()).isEqualTo("tenant-a");
        assertThat(credential.scopes()).isEqualTo(Set.of("usage:read", "billing:write"));
        Instant usedAt = Instant.parse("2026-11-11T12:30:00Z");
        store.recordUse(credentialId, usedAt);
        assertThat(jdbc.queryForObject("SELECT last_used_at FROM api_credentials WHERE id=?", Timestamp.class, credentialId).toInstant()).isEqualTo(usedAt);
    }

    @Test void databaseRejectsCrossTenantUsageAttribution() {
        UUID plan = UUID.randomUUID(), subscription = UUID.randomUUID();
        jdbc.update("INSERT INTO billing_plans(id,tenant_id,plan_key,name,currency) VALUES (?,?,?,?,?)", plan, "tenant-a", "growth", "Growth", "USD");
        jdbc.update("INSERT INTO subscriptions(id,tenant_id,customer_ref,plan_id,status,started_at) VALUES (?,?,?,?,?,now())", subscription, "tenant-a", "customer-1", plan, "ACTIVE");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usage_events(id,tenant_id,subscription_id,external_event_id,event_fingerprint,meter_key,quantity,occurred_at,received_at,attributes_json)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), "tenant-b", subscription, "evt-cross-tenant", "a".repeat(64),
                "api_calls", 1, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
