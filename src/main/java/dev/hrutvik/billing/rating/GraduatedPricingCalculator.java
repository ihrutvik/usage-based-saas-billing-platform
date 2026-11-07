package dev.hrutvik.billing.rating;
import org.springframework.stereotype.Component; import java.math.*; import java.util.*;
@Component
public class GraduatedPricingCalculator {
 public BigDecimal calculate(BigDecimal quantity,List<PriceTier> tiers){
   if(quantity.signum()<0)throw new IllegalArgumentException("quantity cannot be negative");
   if(tiers.isEmpty())throw new IllegalArgumentException("at least one price tier is required");
   var sorted=tiers.stream().sorted(Comparator.comparing(PriceTier::startInclusive)).toList(); validate(sorted);
   BigDecimal amount=BigDecimal.ZERO;
   for(var tier:sorted){if(quantity.compareTo(tier.startInclusive())<=0)break;BigDecimal upper=tier.endExclusive()==null?quantity:quantity.min(tier.endExclusive());BigDecimal units=upper.subtract(tier.startInclusive());if(units.signum()>0)amount=amount.add(units.multiply(tier.unitPrice()));}
   return amount.setScale(2,RoundingMode.HALF_UP);
 }
 private void validate(List<PriceTier> tiers){BigDecimal expected=BigDecimal.ZERO;for(int i=0;i<tiers.size();i++){var t=tiers.get(i);if(t.startInclusive().compareTo(expected)!=0)throw new IllegalArgumentException("price tiers must be contiguous and begin at zero");if(t.unitPrice().signum()<0)throw new IllegalArgumentException("unit price cannot be negative");if(t.endExclusive()==null){if(i!=tiers.size()-1)throw new IllegalArgumentException("open tier must be last");}else{if(t.endExclusive().compareTo(t.startInclusive())<=0)throw new IllegalArgumentException("invalid tier range");expected=t.endExclusive();}}if(tiers.get(tiers.size()-1).endExclusive()!=null)throw new IllegalArgumentException("final tier must be open-ended");}
}
