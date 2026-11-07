package dev.hrutvik.billing.outbox;
import org.junit.jupiter.api.Test; import java.time.*; import java.util.*; import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.Mockito.*;
class OutboxLeaseServiceTest {
 @Test void claimsBatchAndIncrementsAttempt(){var repository=mock(OutboxEventRepository.class);var now=Instant.parse("2026-11-02T10:00:00Z");var event=new OutboxEvent(UUID.randomUUID(),UUID.randomUUID(),"USAGE_RECORDED","tenant-a","{}",now);when(repository.claimable(now,25)).thenReturn(List.of(event));var service=new OutboxLeaseService(repository,Clock.fixed(now,ZoneOffset.UTC));assertThat(service.claim("worker-1",25,Duration.ofSeconds(30))).containsExactly(event);assertThat(event.getPublishAttempts()).isEqualTo(1);}
}
