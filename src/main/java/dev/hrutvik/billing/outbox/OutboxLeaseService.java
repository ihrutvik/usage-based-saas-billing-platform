package dev.hrutvik.billing.outbox;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*; import java.util.*;
@Service
public class OutboxLeaseService {
 private final OutboxEventRepository repository; private final Clock clock;
 public OutboxLeaseService(OutboxEventRepository repository){this(repository,Clock.systemUTC());} OutboxLeaseService(OutboxEventRepository r,Clock c){repository=r;clock=c;}
 @Transactional public List<OutboxEvent> claim(String owner,int batchSize,Duration leaseDuration){Instant now=clock.instant();var events=repository.claimable(now,batchSize);events.forEach(e->e.lease(owner,now.plus(leaseDuration)));return List.copyOf(events);}
 @Transactional public void markPublished(UUID id){repository.findById(id).ifPresent(e->e.published(clock.instant()));}
 @Transactional public void release(UUID id){repository.findById(id).ifPresent(OutboxEvent::release);}
}
