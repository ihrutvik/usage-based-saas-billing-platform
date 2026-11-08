package dev.hrutvik.billing.adjustment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/v1/billing-adjustments")
public class LateUsageAdjustmentController {
    private static final Set<String> STATUSES = Set.of("PENDING", "INVOICED", "WAIVED");
    private final LateUsageAdjustmentStore store; private final LateUsageAdjustmentService service;
    public LateUsageAdjustmentController(LateUsageAdjustmentStore store, LateUsageAdjustmentService service) { this.store=store; this.service=service; }

    @GetMapping
    public List<LateUsageAdjustment> list(@RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
                                          @RequestParam UUID subscriptionId,
                                          @RequestParam(defaultValue="PENDING") String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw new IllegalArgumentException("unsupported adjustment status");
        return store.list(tenantId.trim(), subscriptionId, normalized);
    }

    @PostMapping("/{adjustmentId}/waive")
    public LateUsageAdjustment waive(@RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
                                     @PathVariable UUID adjustmentId,
                                     @Valid @RequestBody WaiveAdjustmentRequest request) {
        return service.waive(tenantId.trim(), adjustmentId, request.reason());
    }
}
