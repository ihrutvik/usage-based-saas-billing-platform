package dev.hrutvik.billing.metering;

import dev.hrutvik.billing.usage.UsageEvent;
import dev.hrutvik.billing.usage.UsageEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealtimeUsageProjectionTest {
    @Test void appliesEventThroughAtomicRedisScript() {
        var events=mock(UsageEventRepository.class); var redis=mock(StringRedisTemplate.class); var meters=new SimpleMeterRegistry();
        var id=UUID.randomUUID(); var subscription=UUID.randomUUID();
        when(events.findById(id)).thenReturn(Optional.of(new UsageEvent(id,"tenant-a",subscription,"ext","fp","api_calls",new BigDecimal("2.50"),Instant.parse("2026-11-09T10:00:00Z"),Instant.now(),"{}")));
        when(redis.execute(any(),anyList(),any(String[].class))).thenReturn("2.5");
        new RealtimeUsageProjection(events,redis,new RealtimeUsageKey(),meters,Duration.ofDays(120),Duration.ofDays(120),false).record(id);
        verify(redis).execute(any(),argThat(keys->keys.size()==2 && keys.get(0).endsWith(id.toString()) && keys.get(1).endsWith(":2026-11")),eq("2.50"),eq("10368000"),eq("10368000"));
        assertEquals(1.0,meters.counter("billing.realtime_usage.events","outcome","projected").count());
    }

    @Test void failsClosedSoKafkaCanRetry() {
        var events=mock(UsageEventRepository.class); var redis=mock(StringRedisTemplate.class); var id=UUID.randomUUID();
        when(events.findById(id)).thenReturn(Optional.of(new UsageEvent(id,"t",UUID.randomUUID(),"e","f","m",BigDecimal.ONE,Instant.now(),Instant.now(),"{}")));
        doThrow(new RuntimeException("redis down")).when(redis)
                .execute(any(),anyList(),anyString(),anyString(),anyString());
        assertThrows(RealtimeUsageUnavailable.class,()->new RealtimeUsageProjection(events,redis,new RealtimeUsageKey(),new SimpleMeterRegistry(),Duration.ofDays(1),Duration.ofDays(1),false).record(id));
    }

    @SuppressWarnings("unchecked")
    @Test void readsCurrentPeriodCounter() {
        var redis=mock(StringRedisTemplate.class); var values=mock(ValueOperations.class); when(redis.opsForValue()).thenReturn(values); when(values.get(anyString())).thenReturn("42.75");
        var projection=new RealtimeUsageProjection(mock(UsageEventRepository.class),redis,new RealtimeUsageKey(),new SimpleMeterRegistry(),Duration.ofDays(1),Duration.ofDays(1),false);
        assertEquals(new BigDecimal("42.75"),projection.current("tenant",UUID.randomUUID(),"api_calls",YearMonth.of(2026,11)));
    }
}
