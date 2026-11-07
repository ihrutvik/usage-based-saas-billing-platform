package dev.hrutvik.billing.credit;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record CreditRequest(@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal amount,@NotBlank @Size(max=500) String reason) {}
