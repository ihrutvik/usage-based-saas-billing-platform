package dev.hrutvik.billing.adjustment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LateUsageAdjustment(UUID id, UUID subscriptionId, UUID invoiceId, String invoiceNumber,
    UUID usageEventId, String meterKey, BigDecimal quantity, Instant occurredAt, String status,
    Instant detectedAt, Instant resolvedAt, String resolutionReason) {}
