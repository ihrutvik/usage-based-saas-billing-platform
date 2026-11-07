package dev.hrutvik.billing.usage;
import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import org.springframework.http.*; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/v1/usage-events") @Validated
public class UsageEventController { private final UsageEventService service; public UsageEventController(UsageEventService s){service=s;} @PostMapping public ResponseEntity<UsageEventResponse> ingest(@RequestHeader("X-Tenant-Id") @NotBlank String tenant,@Valid @RequestBody UsageEventRequest request){var result=service.ingest(tenant,request); return ResponseEntity.status(result.replayed()?HttpStatus.OK:HttpStatus.CREATED).body(result);} }
