package dev.hrutvik.billing.adjustment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class LateUsageAdjustmentStore {
    private final JdbcTemplate jdbc;
    public LateUsageAdjustmentStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<LateUsageAdjustment> list(String tenantId, UUID subscriptionId, String status) {
        return jdbc.query("""
            SELECT a.id,a.subscription_id,a.invoice_id,i.invoice_number,a.usage_event_id,a.meter_key,
                   a.quantity,a.occurred_at,a.status,a.detected_at,a.resolved_at,a.resolution_reason
              FROM late_usage_adjustments a JOIN invoices i ON i.id=a.invoice_id
             WHERE a.tenant_id=? AND a.subscription_id=? AND a.status=?
             ORDER BY a.detected_at
            """, (rs, row) -> map(rs), tenantId, subscriptionId, status);
    }

    public LateUsageAdjustment lockPending(String tenantId, UUID adjustmentId) {
        return jdbc.query("""
            SELECT a.id,a.subscription_id,a.invoice_id,i.invoice_number,a.usage_event_id,a.meter_key,
                   a.quantity,a.occurred_at,a.status,a.detected_at,a.resolved_at,a.resolution_reason
              FROM late_usage_adjustments a JOIN invoices i ON i.id=a.invoice_id
             WHERE a.id=? AND a.tenant_id=? AND a.status='PENDING' FOR UPDATE OF a
            """, (rs, row) -> map(rs), adjustmentId, tenantId).stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("pending late usage adjustment not found"));
    }

    public void waive(UUID adjustmentId, String reason, Instant resolvedAt) {
        int updated = jdbc.update("UPDATE late_usage_adjustments SET status='WAIVED',resolution_reason=?,resolved_at=? WHERE id=? AND status='PENDING'",
            reason, resolvedAt, adjustmentId);
        if (updated != 1) throw new IllegalStateException("late usage adjustment changed concurrently");
    }

    private LateUsageAdjustment map(java.sql.ResultSet rs) throws java.sql.SQLException {
        var resolved = rs.getTimestamp("resolved_at");
        return new LateUsageAdjustment(rs.getObject("id", UUID.class), rs.getObject("subscription_id", UUID.class),
            rs.getObject("invoice_id", UUID.class), rs.getString("invoice_number"), rs.getObject("usage_event_id", UUID.class),
            rs.getString("meter_key"), rs.getBigDecimal("quantity"), rs.getTimestamp("occurred_at").toInstant(),
            rs.getString("status"), rs.getTimestamp("detected_at").toInstant(), resolved == null ? null : resolved.toInstant(),
            rs.getString("resolution_reason"));
    }
}
