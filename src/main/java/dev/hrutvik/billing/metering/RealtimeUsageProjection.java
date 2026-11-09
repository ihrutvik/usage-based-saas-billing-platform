package dev.hrutvik.billing.metering;

import dev.hrutvik.billing.usage.UsageEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class RealtimeUsageProjection {
    private static final DefaultRedisScript<String> INCREMENT_ONCE = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[2]) then
              local total = redis.call('INCRBYFLOAT', KEYS[2], ARGV[1])
              redis.call('EXPIRE', KEYS[2], ARGV[3])
              return total
            end
            return redis.call('GET', KEYS[2]) or '0'
            """, String.class);

    private final UsageEventRepository events;
    private final StringRedisTemplate redis;
    private final RealtimeUsageKey keys;
    private final long eventTtlSeconds;
    private final long counterTtlSeconds;
    private final boolean failOpen;
    private final MeterRegistry meters;

    public RealtimeUsageProjection(UsageEventRepository events, StringRedisTemplate redis,
                                   RealtimeUsageKey keys, MeterRegistry meters,
                                   @Value("${billing.realtime-usage.event-ttl:P120D}") Duration eventTtl,
                                   @Value("${billing.realtime-usage.counter-ttl:P120D}") Duration counterTtl,
                                   @Value("${billing.realtime-usage.fail-open:false}") boolean failOpen) {
        this.events = events;
        this.redis = redis;
        this.keys = keys;
        this.meters = meters;
        this.eventTtlSeconds = eventTtl.toSeconds();
        this.counterTtlSeconds = counterTtl.toSeconds();
        this.failOpen = failOpen;
    }

    public void record(UUID eventId) {
        var event = events.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Usage event missing for real-time projection: " + eventId));
        var period = YearMonth.from(event.getOccurredAt().atZone(ZoneOffset.UTC));
        try {
            redis.execute(INCREMENT_ONCE,
                    List.of(keys.event(eventId), keys.counter(event.getTenantId(), event.getSubscriptionId(), event.getMeterKey(), period)),
                    event.getQuantity().toPlainString(), Long.toString(eventTtlSeconds), Long.toString(counterTtlSeconds));
            meters.counter("billing.realtime_usage.events", "outcome", "projected").increment();
        } catch (RuntimeException failure) {
            meters.counter("billing.realtime_usage.events", "outcome", failOpen ? "degraded" : "retry").increment();
            if (!failOpen) throw new RealtimeUsageUnavailable("Redis projection failed; Kafka delivery will be retried", failure);
        }
    }

    public BigDecimal current(String tenantId, UUID subscriptionId, String meterKey, YearMonth period) {
        try {
            String value = redis.opsForValue().get(keys.counter(tenantId, subscriptionId, meterKey, period));
            return value == null ? BigDecimal.ZERO : new BigDecimal(value);
        } catch (RuntimeException failure) {
            throw new RealtimeUsageUnavailable("Real-time usage is temporarily unavailable", failure);
        }
    }
}
