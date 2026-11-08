package dev.hrutvik.billing.adjustment;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hrutvik.billing.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LateUsageAdjustmentServiceTest {
 @Test void waivesPendingAdjustmentAndPublishesAuditEvent(){var store=mock(LateUsageAdjustmentStore.class);var outbox=mock(OutboxEventRepository.class);var now=Instant.parse("2026-11-08T12:00:00Z");var id=UUID.randomUUID();var pending=new LateUsageAdjustment(id,UUID.randomUUID(),UUID.randomUUID(),"INV-1",UUID.randomUUID(),"api_calls",new BigDecimal("25"),now.minusSeconds(3600),"PENDING",now.minusSeconds(60),null,null);when(store.lockPending("tenant-a",id)).thenReturn(pending);var service=new LateUsageAdjustmentService(store,outbox,new ObjectMapper(),Clock.fixed(now,ZoneOffset.UTC));var result=service.waive("tenant-a",id,"Customer goodwill");assertThat(result.status()).isEqualTo("WAIVED");assertThat(result.resolutionReason()).isEqualTo("Customer goodwill");verify(store).waive(id,"Customer goodwill",now);verify(outbox).save(argThat(e->"LATE_USAGE_WAIVED".equals(e.getEventType())));}
}
