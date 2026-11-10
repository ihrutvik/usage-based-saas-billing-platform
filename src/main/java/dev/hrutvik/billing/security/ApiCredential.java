package dev.hrutvik.billing.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ApiCredential(UUID id, String keyId, String secretHash, String tenantId,
                            Set<String> scopes, boolean active, Instant expiresAt) {
    boolean isUsable(Instant now) { return active && (expiresAt == null || expiresAt.isAfter(now)); }
}
