package dev.hrutvik.billing.metering;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record RealtimeUsageResponse(UUID subscriptionId, String meterKey, YearMonth period, BigDecimal quantity) {}
