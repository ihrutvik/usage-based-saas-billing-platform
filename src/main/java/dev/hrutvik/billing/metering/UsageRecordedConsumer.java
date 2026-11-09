package dev.hrutvik.billing.metering;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class UsageRecordedConsumer {
    private final UsageAggregationService aggregation;
    private final RealtimeUsageProjection realtimeUsage;
    public UsageRecordedConsumer(UsageAggregationService aggregation, RealtimeUsageProjection realtimeUsage) {
        this.aggregation = aggregation;
        this.realtimeUsage = realtimeUsage;
    }

    @KafkaListener(topics="${billing.outbox.topic:billing.usage-recorded.v1}", groupId="${billing.metering.group-id:billing-metering-v1}")
    public void consume(ConsumerRecord<String,String> record, Acknowledgment acknowledgment) {
        var header = record.headers().lastHeader("event-id");
        if (header == null) throw new IllegalArgumentException("Missing event-id header");
        UUID eventId = UUID.fromString(new String(header.value(), StandardCharsets.UTF_8));
        aggregation.aggregate(eventId, record.topic(), record.partition(), record.offset());
        realtimeUsage.record(eventId);
        acknowledgment.acknowledge();
    }
}
