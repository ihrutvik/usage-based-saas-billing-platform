package dev.hrutvik.billing.invoice;
import java.math.BigDecimal; import java.time.Instant; import java.util.*;
public record InvoiceResponse(UUID invoiceId,String invoiceNumber,UUID subscriptionId,String currency,Instant periodStart,Instant periodEnd,BigDecimal total,String status,List<InvoiceLineView> lines,boolean replayed) {}
