package dev.hrutvik.billing.metering;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/v1/realtime-usage")
public class RealtimeUsageController {
    private final RealtimeUsageProjection projection;
    public RealtimeUsageController(RealtimeUsageProjection projection) { this.projection = projection; }

    @GetMapping
    public RealtimeUsageResponse current(@RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
                                         @RequestParam UUID subscriptionId,
                                         @RequestParam @NotBlank String meterKey,
                                         @RequestParam String period) {
        try {
            var normalizedMeter = meterKey.trim().toLowerCase(Locale.ROOT);
            var month = YearMonth.parse(period);
            return new RealtimeUsageResponse(subscriptionId, normalizedMeter, month,
                    projection.current(tenantId.trim(), subscriptionId, normalizedMeter, month));
        } catch (DateTimeParseException invalid) {
            throw new IllegalArgumentException("period must use YYYY-MM format", invalid);
        }
    }
}
