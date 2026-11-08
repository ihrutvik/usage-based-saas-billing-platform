package dev.hrutvik.billing.metering;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeteringStore {
    private final JdbcTemplate jdbc;
    public MeteringStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean recordInbox(UUID eventId, String topic, int partition, long offset, Instant processedAt) {
        return jdbc.update("""
            INSERT INTO metering_inbox(event_id, source_topic, source_partition, source_offset, processed_at)
            VALUES (?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING
            """, eventId, topic, partition, offset, processedAt) == 1;
    }

    public void addToBucket(String tenantId, UUID subscriptionId, String meterKey, Instant bucketStart, BigDecimal quantity, Instant updatedAt) {
        jdbc.update("""
            INSERT INTO subscription_usage_hourly_buckets(tenant_id, subscription_id, meter_key, bucket_start, quantity, event_count, updated_at)
            VALUES (?, ?, ?, ?, ?, 1, ?)
            ON CONFLICT (tenant_id, subscription_id, meter_key, bucket_start) DO UPDATE
              SET quantity = subscription_usage_hourly_buckets.quantity + EXCLUDED.quantity,
                  event_count = subscription_usage_hourly_buckets.event_count + 1,
                  updated_at = EXCLUDED.updated_at
            """, tenantId, subscriptionId, meterKey, bucketStart, quantity, updatedAt);
    }

    public UsageSummary summarize(String tenantId, UUID subscriptionId, String meterKey, Instant from, Instant to) {
        return jdbc.queryForObject("""
            SELECT COALESCE(SUM(quantity), 0) AS quantity, COALESCE(SUM(event_count), 0) AS event_count
              FROM subscription_usage_hourly_buckets
             WHERE tenant_id = ? AND subscription_id = ? AND meter_key = ? AND bucket_start >= ? AND bucket_start < ?
            """, (rs, row) -> new UsageSummary(tenantId, subscriptionId, meterKey, from, to,
                rs.getBigDecimal("quantity"), rs.getLong("event_count")), tenantId, subscriptionId, meterKey, from, to);
    }

    public Optional<FinalizedInvoice> finalizedInvoiceFor(String tenantId, UUID subscriptionId, Instant occurredAt, Instant receivedAt) {
        return jdbc.query("""
            SELECT id, invoice_number, currency, period_start, period_end
              FROM invoices
             WHERE tenant_id = ? AND subscription_id = ? AND status = 'FINALIZED'
               AND period_start <= ? AND period_end > ? AND finalized_at < ?
             ORDER BY finalized_at DESC LIMIT 1
            """, (rs, row) -> new FinalizedInvoice(rs.getObject("id", UUID.class), rs.getString("invoice_number"),
                rs.getString("currency"), rs.getTimestamp("period_start").toInstant(), rs.getTimestamp("period_end").toInstant()),
            tenantId, subscriptionId, occurredAt, occurredAt, receivedAt).stream().findFirst();
    }

    public boolean recordLateAdjustment(UUID id, FinalizedInvoice invoice, String tenantId, UUID subscriptionId,
                                        UUID usageEventId, String meterKey, BigDecimal quantity,
                                        Instant occurredAt, Instant detectedAt) {
        return jdbc.update("""
            INSERT INTO late_usage_adjustments(id, tenant_id, subscription_id, invoice_id, usage_event_id,
              meter_key, quantity, occurred_at, status, detected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
            ON CONFLICT (usage_event_id) DO NOTHING
            """, id, tenantId, subscriptionId, invoice.id(), usageEventId, meterKey, quantity, occurredAt, detectedAt) == 1;
    }

    public record FinalizedInvoice(UUID id, String invoiceNumber, String currency, Instant periodStart, Instant periodEnd) {}
}
