package dev.hrutvik.billing.adjustment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hrutvik.billing.outbox.OutboxEvent;
import dev.hrutvik.billing.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class LateUsageAdjustmentService {
    private final LateUsageAdjustmentStore store; private final OutboxEventRepository outbox; private final ObjectMapper json; private final Clock clock;
    public LateUsageAdjustmentService(LateUsageAdjustmentStore store, OutboxEventRepository outbox, ObjectMapper json) { this(store, outbox, json, Clock.systemUTC()); }
    LateUsageAdjustmentService(LateUsageAdjustmentStore store, OutboxEventRepository outbox, ObjectMapper json, Clock clock) { this.store=store; this.outbox=outbox; this.json=json; this.clock=clock; }

    @Transactional
    public LateUsageAdjustment waive(String tenantId, UUID adjustmentId, String reason) {
        var pending = store.lockPending(tenantId, adjustmentId);
        var now = clock.instant();
        String normalized = reason.trim();
        store.waive(adjustmentId, normalized, now);
        try {
            outbox.save(new OutboxEvent(UUID.randomUUID(), adjustmentId, "LATE_USAGE_WAIVED", tenantId,
                "billing.late-usage-waived.v1", json.writeValueAsString(Map.of("adjustmentId", adjustmentId,
                    "invoiceId", pending.invoiceId(), "usageEventId", pending.usageEventId(), "reason", normalized)), now));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize adjustment waiver", e); }
        return new LateUsageAdjustment(pending.id(), pending.subscriptionId(), pending.invoiceId(), pending.invoiceNumber(),
            pending.usageEventId(), pending.meterKey(), pending.quantity(), pending.occurredAt(), "WAIVED",
            pending.detectedAt(), now, normalized);
    }
}
