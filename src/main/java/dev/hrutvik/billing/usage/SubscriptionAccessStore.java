package dev.hrutvik.billing.usage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class SubscriptionAccessStore {
    private final JdbcTemplate jdbc;
    public SubscriptionAccessStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActiveForTenant(String tenantId, UUID subscriptionId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM subscriptions WHERE id=? AND tenant_id=? AND status='ACTIVE')
            """, Boolean.class, subscriptionId, tenantId));
    }
}
