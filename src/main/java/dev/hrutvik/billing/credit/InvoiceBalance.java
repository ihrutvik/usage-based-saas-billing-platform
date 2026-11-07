package dev.hrutvik.billing.credit;
import java.math.BigDecimal; import java.util.UUID;
public record InvoiceBalance(UUID invoiceId,String currency,BigDecimal invoiceTotal,BigDecimal creditedTotal) {public BigDecimal remaining(){return invoiceTotal.subtract(creditedTotal);}}
