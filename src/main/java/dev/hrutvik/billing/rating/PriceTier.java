package dev.hrutvik.billing.rating;
import java.math.BigDecimal;
public record PriceTier(BigDecimal startInclusive, BigDecimal endExclusive, BigDecimal unitPrice) {}
