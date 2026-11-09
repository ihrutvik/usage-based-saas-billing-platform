package dev.hrutvik.billing.metering;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Component
class RealtimeUsageKey {
    String counter(String tenantId, UUID subscriptionId, String meterKey, YearMonth period) {
        return "billing:usage:rt:" + segment(tenantId.trim()) + ":" + subscriptionId + ":"
                + segment(meterKey.trim().toLowerCase(Locale.ROOT)) + ":" + period;
    }

    String event(UUID eventId) {
        return "billing:usage:rt:event:" + eventId;
    }

    private String segment(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
