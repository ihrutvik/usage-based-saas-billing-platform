package dev.hrutvik.billing.metering;
import org.apache.kafka.clients.consumer.ConsumerRecord; import org.apache.kafka.common.header.internals.RecordHeader; import org.junit.jupiter.api.Test; import org.springframework.kafka.support.Acknowledgment; import java.nio.charset.StandardCharsets; import java.util.UUID; import static org.mockito.Mockito.*;
class UsageRecordedConsumerTest {
 @Test void acknowledgesOnlyAfterAggregation(){var aggregation=mock(UsageAggregationService.class);var ack=mock(Acknowledgment.class);var id=UUID.randomUUID();var record=new ConsumerRecord<String,String>("usage",3,19,"key","{}");record.headers().add(new RecordHeader("event-id",id.toString().getBytes(StandardCharsets.UTF_8)));new UsageRecordedConsumer(aggregation).consume(record,ack);var order=inOrder(aggregation,ack);order.verify(aggregation).aggregate(id,"usage",3,19);order.verify(ack).acknowledge();}
}
