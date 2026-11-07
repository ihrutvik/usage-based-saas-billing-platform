package dev.hrutvik.billing.usage;
import org.springframework.stereotype.Component; import java.nio.charset.StandardCharsets; import java.security.*; import java.util.*;
@Component
public class UsageFingerprint {
  public String create(UsageEventRequest r){
    var attributes=new TreeMap<>(r.attributes()==null?Map.<String,String>of():r.attributes());
    String canonical=r.subscriptionId()+"|"+r.meterKey().trim().toLowerCase(Locale.ROOT)+"|"+r.quantity().stripTrailingZeros().toPlainString()+"|"+r.occurredAt()+"|"+attributes;
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); } catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
}
