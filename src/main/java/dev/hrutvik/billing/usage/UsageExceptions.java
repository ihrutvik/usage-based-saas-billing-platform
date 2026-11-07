package dev.hrutvik.billing.usage;
class IdempotencyConflict extends RuntimeException { IdempotencyConflict(){super("Event ID was already used with a different payload");} }
class UnknownMeter extends RuntimeException { UnknownMeter(String key){super("Unknown or inactive meter: "+key);} }
class EventOutsideWindow extends RuntimeException { EventOutsideWindow(){super("occurredAt is outside the accepted ingestion window");} }
