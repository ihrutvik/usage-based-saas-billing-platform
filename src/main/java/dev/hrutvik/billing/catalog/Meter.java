package dev.hrutvik.billing.catalog;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="billing_meters")
public class Meter { @Id private UUID id; @Column(name="tenant_id") private String tenantId; @Column(name="meter_key") private String meterKey; private String unit; private boolean active; protected Meter(){} public String getMeterKey(){return meterKey;} }
