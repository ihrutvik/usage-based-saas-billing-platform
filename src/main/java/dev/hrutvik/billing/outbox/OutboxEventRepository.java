package dev.hrutvik.billing.outbox;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.Instant; import java.util.*;
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,UUID>{
 @Query(value="SELECT * FROM outbox_events WHERE published_at IS NULL AND (leased_until IS NULL OR leased_until < :now) ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED",nativeQuery=true)
 List<OutboxEvent> claimable(@Param("now") Instant now,@Param("batchSize") int batchSize);
}
