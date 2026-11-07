package dev.hrutvik.billing.usage;
import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import dev.hrutvik.billing.catalog.MeterRepository; import dev.hrutvik.billing.outbox.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.security.MessageDigest; import java.time.*; import java.util.*;
@Service
public class UsageEventService {
 private final UsageEventRepository events; private final MeterRepository meters; private final SubscriptionAccessStore subscriptions; private final OutboxEventRepository outbox; private final UsageFingerprint fingerprints; private final ObjectMapper json; private final Clock clock; private final Duration maxAge; private final Duration futureSkew;
 public UsageEventService(UsageEventRepository events,MeterRepository meters,SubscriptionAccessStore subscriptions,OutboxEventRepository outbox,UsageFingerprint fingerprints,ObjectMapper json,@Value("${billing.ingestion.max-age:P7D}") Duration maxAge,@Value("${billing.ingestion.future-skew:PT5M}") Duration futureSkew){this(events,meters,subscriptions,outbox,fingerprints,json,Clock.systemUTC(),maxAge,futureSkew);}
 UsageEventService(UsageEventRepository e,MeterRepository m,SubscriptionAccessStore s,OutboxEventRepository o,UsageFingerprint f,ObjectMapper j,Clock c,Duration a,Duration skew){events=e;meters=m;subscriptions=s;outbox=o;fingerprints=f;json=j;clock=c;maxAge=a;futureSkew=skew;}
 @Transactional public UsageEventResponse ingest(String tenantId,UsageEventRequest request){
   String tenant=tenantId.trim(); String meter=request.meterKey().trim().toLowerCase(Locale.ROOT); String fp=fingerprints.create(request);
   var prior=events.findByTenantIdAndExternalEventId(tenant,request.eventId()); if(prior.isPresent()){if(!MessageDigest.isEqual(prior.get().getEventFingerprint().getBytes(),fp.getBytes()))throw new IdempotencyConflict(); return response(prior.get(),true);}
   Instant now=clock.instant(); if(request.occurredAt().isBefore(now.minus(maxAge))||request.occurredAt().isAfter(now.plus(futureSkew)))throw new EventOutsideWindow();
   if(!subscriptions.isActiveForTenant(tenant,request.subscriptionId()))throw new IllegalArgumentException("active subscription not found for tenant");
   meters.findByTenantIdAndMeterKeyAndActiveTrue(tenant,meter).orElseThrow(()->new UnknownMeter(meter));
   try { UUID id=UUID.randomUUID(); var event=new UsageEvent(id,tenant,request.subscriptionId(),request.eventId(),fp,meter,request.quantity(),request.occurredAt(),now,json.writeValueAsString(request.attributes()==null?Map.of():request.attributes())); events.save(event); outbox.save(new OutboxEvent(UUID.randomUUID(),id,"USAGE_RECORDED",tenant,json.writeValueAsString(Map.of("usageEventId",id,"subscriptionId",request.subscriptionId(),"meterKey",meter)),now)); return response(event,false); } catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize usage event",e);}
 }
 private UsageEventResponse response(UsageEvent e,boolean replayed){return new UsageEventResponse(e.getId(),e.getExternalEventId(),e.getSubscriptionId(),e.getMeterKey(),e.getQuantity(),e.getOccurredAt(),replayed);}
}
