package dev.hrutvik.billing.catalog;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface MeterRepository extends JpaRepository<Meter,UUID>{Optional<Meter> findByTenantIdAndMeterKeyAndActiveTrue(String tenantId,String meterKey);}
