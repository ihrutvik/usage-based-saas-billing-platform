package dev.hrutvik.billing.rating;
import org.junit.jupiter.api.Test; import java.math.BigDecimal; import java.util.List; import static org.assertj.core.api.Assertions.*;
class GraduatedPricingCalculatorTest {
 private final GraduatedPricingCalculator calculator=new GraduatedPricingCalculator();
 private List<PriceTier> tiers(){return List.of(new PriceTier(new BigDecimal("0"),new BigDecimal("100"),new BigDecimal("0.10")),new PriceTier(new BigDecimal("100"),new BigDecimal("200"),new BigDecimal("0.08")),new PriceTier(new BigDecimal("200"),null,new BigDecimal("0.05")));}
 @Test void chargesOnlyUnitsInsideEachGraduatedTier(){assertThat(calculator.calculate(new BigDecimal("250"),tiers())).isEqualByComparingTo("20.50");}
 @Test void handlesExactBoundaryWithoutDoubleCharging(){assertThat(calculator.calculate(new BigDecimal("100"),tiers())).isEqualByComparingTo("10.00");}
 @Test void roundsFinalMeterAmountHalfUp(){assertThat(calculator.calculate(new BigDecimal("0.055"),List.of(new PriceTier(BigDecimal.ZERO,null,BigDecimal.ONE)))).isEqualByComparingTo("0.06");}
 @Test void rejectsGapsThatWouldLeaveUsageUnpriced(){var invalid=List.of(new PriceTier(BigDecimal.ZERO,new BigDecimal("10"),BigDecimal.ONE),new PriceTier(new BigDecimal("11"),null,BigDecimal.ONE));assertThatThrownBy(()->calculator.calculate(new BigDecimal("20"),invalid)).isInstanceOf(IllegalArgumentException.class);}
}
