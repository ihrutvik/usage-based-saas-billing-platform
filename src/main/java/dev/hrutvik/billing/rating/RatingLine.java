package dev.hrutvik.billing.rating;
import java.math.BigDecimal; import java.util.List;
public record RatingLine(String meterKey,BigDecimal quantity,BigDecimal amount,List<PriceTier> appliedTiers) {}
