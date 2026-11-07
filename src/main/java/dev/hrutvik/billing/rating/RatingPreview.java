package dev.hrutvik.billing.rating;
import java.math.BigDecimal; import java.time.Instant; import java.util.*;
public record RatingPreview(UUID subscriptionId,String currency,Instant from,Instant to,List<RatingLine> lines,BigDecimal total) {}
