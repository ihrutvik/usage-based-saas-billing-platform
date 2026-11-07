package dev.hrutvik.billing.rating;
import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.UUID;
@RestController @RequestMapping("/v1/rating-previews")
public class RatingController {private final RatingService service;public RatingController(RatingService service){this.service=service;}@GetMapping public RatingPreview preview(@RequestHeader("X-Tenant-Id") String tenantId,@RequestParam UUID subscriptionId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){return service.preview(tenantId.trim(),subscriptionId,from,to);}}
