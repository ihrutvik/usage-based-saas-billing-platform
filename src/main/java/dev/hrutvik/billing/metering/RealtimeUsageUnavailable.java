package dev.hrutvik.billing.metering;

public class RealtimeUsageUnavailable extends RuntimeException {
    public RealtimeUsageUnavailable(String message, Throwable cause) { super(message, cause); }
}
