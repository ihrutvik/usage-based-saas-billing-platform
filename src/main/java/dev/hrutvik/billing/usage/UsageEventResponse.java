package dev.hrutvik.billing.usage;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record UsageEventResponse(UUID id,String eventId,UUID subscriptionId,String meterKey,BigDecimal quantity,Instant occurredAt,boolean replayed) {}
