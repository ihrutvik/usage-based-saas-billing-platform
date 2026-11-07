package dev.hrutvik.billing.metering;

import dev.hrutvik.billing.usage.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UsageAggregationService {
    private final UsageEventRepository events; private final MeteringStore store; private final Clock clock;
    public UsageAggregationService(UsageEventRepository events, MeteringStore store) { this(events, store, Clock.systemUTC()); }
    UsageAggregationService(UsageEventRepository events, MeteringStore store, Clock clock) { this.events=events; this.store=store; this.clock=clock; }

    @Transactional
    public boolean aggregate(UUID eventId, String topic, int partition, long offset) {
        var event = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Usage event not found: " + eventId));
        Instant now = clock.instant();
        if (!store.recordInbox(eventId, topic, partition, offset, now)) return false;
        if (event.getSubscriptionId() == null) throw new IllegalStateException("Usage event lacks subscription attribution: " + eventId);
        Instant bucket = event.getOccurredAt().truncatedTo(ChronoUnit.HOURS);
        store.addToBucket(event.getTenantId(), event.getSubscriptionId(), event.getMeterKey(), bucket, event.getQuantity(), now);
        return true;
    }
}
