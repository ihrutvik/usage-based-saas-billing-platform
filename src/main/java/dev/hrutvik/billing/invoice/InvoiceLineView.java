package dev.hrutvik.billing.invoice;
import java.math.BigDecimal;
public record InvoiceLineView(String meterKey,BigDecimal quantity,BigDecimal amount) {}
