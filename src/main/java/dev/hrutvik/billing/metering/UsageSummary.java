package dev.hrutvik.billing.metering;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record UsageSummary(String tenantId, UUID subscriptionId, String meterKey, Instant from, Instant to, BigDecimal quantity, long eventCount) {}
