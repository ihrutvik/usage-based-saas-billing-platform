package dev.hrutvik.billing.rating;
import java.util.List; import java.util.UUID;
public record MeterPrice(UUID meterId,String meterKey,List<PriceTier> tiers) {}
