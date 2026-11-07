package dev.hrutvik.billing.metering;
import jakarta.validation.constraints.NotBlank; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.Locale; import java.util.UUID;
@RestController @RequestMapping("/v1/usage-summary")
public class UsageSummaryController {
 private final MeteringStore store; public UsageSummaryController(MeteringStore store){this.store=store;}
 @GetMapping public UsageSummary summarize(@RequestHeader("X-Tenant-Id") @NotBlank String tenantId,@RequestParam UUID subscriptionId,@RequestParam @NotBlank String meterKey,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){if(!from.isBefore(to))throw new IllegalArgumentException("from must be before to");return store.summarize(tenantId.trim(),subscriptionId,meterKey.trim().toLowerCase(Locale.ROOT),from,to);}
}
