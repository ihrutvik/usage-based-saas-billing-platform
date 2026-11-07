package dev.hrutvik.billing.usage;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.Map; import java.util.UUID;
public record UsageEventRequest(@NotBlank @Size(max=120) String eventId,@NotNull UUID subscriptionId,@NotBlank @Size(max=80) String meterKey,@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal quantity,@NotNull Instant occurredAt,Map<String,String> attributes) {}
