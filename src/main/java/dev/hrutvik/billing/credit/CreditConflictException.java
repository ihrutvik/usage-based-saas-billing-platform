package dev.hrutvik.billing.credit;
public class CreditConflictException extends RuntimeException {public CreditConflictException(){super("Idempotency key was already used with a different credit request");}}
