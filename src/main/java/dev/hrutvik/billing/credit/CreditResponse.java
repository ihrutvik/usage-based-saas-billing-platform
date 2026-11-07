package dev.hrutvik.billing.credit;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record CreditResponse(UUID creditNoteId,String creditNumber,UUID invoiceId,String currency,BigDecimal amount,String reason,Instant createdAt,boolean replayed) {}
