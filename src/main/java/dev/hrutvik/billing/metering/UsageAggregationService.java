package dev.hrutvik.billing.metering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hrutvik.billing.outbox.OutboxEvent;
import dev.hrutvik.billing.outbox.OutboxEventRepository;
import dev.hrutvik.billing.usage.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UsageAggregationService {
    private final UsageEventRepository events; private final MeteringStore store; private final OutboxEventRepository outbox; private final ObjectMapper json; private final Clock clock;
    public UsageAggregationService(UsageEventRepository events, MeteringStore store, OutboxEventRepository outbox, ObjectMapper json) { this(events, store, outbox, json, Clock.systemUTC()); }
    UsageAggregationService(UsageEventRepository events, MeteringStore store, OutboxEventRepository outbox, ObjectMapper json, Clock clock) { this.events=events; this.store=store; this.outbox=outbox; this.json=json; this.clock=clock; }

    @Transactional
    public boolean aggregate(UUID eventId, String topic, int partition, long offset) {
        var event = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Usage event not found: " + eventId));
        Instant now = clock.instant();
        if (!store.recordInbox(eventId, topic, partition, offset, now)) return false;
        if (event.getSubscriptionId() == null) throw new IllegalStateException("Usage event lacks subscription attribution: " + eventId);
        Instant bucket = event.getOccurredAt().truncatedTo(ChronoUnit.HOURS);
        store.addToBucket(event.getTenantId(), event.getSubscriptionId(), event.getMeterKey(), bucket, event.getQuantity(), now);
        store.finalizedInvoiceFor(event.getTenantId(), event.getSubscriptionId(), event.getOccurredAt(), event.getReceivedAt())
            .ifPresent(invoice -> recordLateUsage(eventId, event.getTenantId(), event.getSubscriptionId(),
                event.getMeterKey(), event.getQuantity(), event.getOccurredAt(), invoice, now));
        return true;
    }

    private void recordLateUsage(UUID eventId, String tenantId, UUID subscriptionId, String meterKey,
                                 java.math.BigDecimal quantity, Instant occurredAt,
                                 MeteringStore.FinalizedInvoice invoice, Instant now) {
        UUID adjustmentId = UUID.randomUUID();
        if (!store.recordLateAdjustment(adjustmentId, invoice, tenantId, subscriptionId, eventId, meterKey, quantity, occurredAt, now)) return;
        try {
            String payload = json.writeValueAsString(java.util.Map.of(
                "adjustmentId", adjustmentId, "invoiceId", invoice.id(), "invoiceNumber", invoice.invoiceNumber(),
                "usageEventId", eventId, "subscriptionId", subscriptionId, "meterKey", meterKey,
                "quantity", quantity, "occurredAt", occurredAt));
            outbox.save(new OutboxEvent(UUID.randomUUID(), adjustmentId, "LATE_USAGE_DETECTED", tenantId,
                "billing.late-usage-detected.v1", payload, now));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize late usage adjustment", e);
        }
    }
}
