package dev.hrutvik.billing.usage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> { Optional<UsageEvent> findByTenantIdAndExternalEventId(String tenantId,String externalEventId); }
