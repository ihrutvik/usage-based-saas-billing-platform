package dev.hrutvik.billing.rating;
import java.util.UUID;
public record SubscriptionPlan(UUID subscriptionId,UUID planId,String currency) {}
