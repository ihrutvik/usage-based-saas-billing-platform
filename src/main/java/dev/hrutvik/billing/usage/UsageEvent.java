package dev.hrutvik.billing.usage;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "usage_events")
public class UsageEvent {
    @Id private UUID id;
    @Column(name="tenant_id", nullable=false) private String tenantId;
    @Column(name="subscription_id") private UUID subscriptionId;
    @Column(name="external_event_id", nullable=false) private String externalEventId;
    @Column(name="event_fingerprint", nullable=false, length=64) private String eventFingerprint;
    @Column(name="meter_key", nullable=false) private String meterKey;
    @Column(nullable=false, precision=24, scale=8) private BigDecimal quantity;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    @Column(name="received_at", nullable=false) private Instant receivedAt;
    @Column(name="attributes_json", nullable=false, columnDefinition="text") private String attributesJson;
    protected UsageEvent() {}
    public UsageEvent(UUID id,String tenantId,UUID subscriptionId,String externalEventId,String fingerprint,String meterKey,BigDecimal quantity,Instant occurredAt,Instant receivedAt,String attributesJson){this.id=id;this.tenantId=tenantId;this.subscriptionId=subscriptionId;this.externalEventId=externalEventId;this.eventFingerprint=fingerprint;this.meterKey=meterKey;this.quantity=quantity;this.occurredAt=occurredAt;this.receivedAt=receivedAt;this.attributesJson=attributesJson;}
    public UUID getId(){return id;} public String getTenantId(){return tenantId;} public UUID getSubscriptionId(){return subscriptionId;} public String getEventFingerprint(){return eventFingerprint;} public String getExternalEventId(){return externalEventId;} public String getMeterKey(){return meterKey;} public BigDecimal getQuantity(){return quantity;} public Instant getOccurredAt(){return occurredAt;} public Instant getReceivedAt(){return receivedAt;}
}
