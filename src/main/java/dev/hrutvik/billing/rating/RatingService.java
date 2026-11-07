package dev.hrutvik.billing.rating;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Service
public class RatingService {
 private final RatingStore store; private final GraduatedPricingCalculator calculator; public RatingService(RatingStore store,GraduatedPricingCalculator calculator){this.store=store;this.calculator=calculator;}
 @Transactional(readOnly=true) public RatingPreview preview(String tenantId,UUID subscriptionId,Instant from,Instant to){if(!from.isBefore(to))throw new IllegalArgumentException("from must be before to");var plan=store.subscription(tenantId,subscriptionId);var lines=store.prices(plan.planId()).stream().map(p->{var quantity=store.quantity(tenantId,subscriptionId,p.meterKey(),from,to);return new RatingLine(p.meterKey(),quantity,calculator.calculate(quantity,p.tiers()),p.tiers());}).toList();var total=lines.stream().map(RatingLine::amount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,java.math.RoundingMode.HALF_UP);return new RatingPreview(subscriptionId,plan.currency(),from,to,lines,total);}
}
