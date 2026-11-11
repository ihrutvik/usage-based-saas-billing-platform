package dev.hrutvik.billing.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

@Repository
public class ApiCredentialStore {
    private final JdbcTemplate jdbc;
    public ApiCredentialStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<ApiCredential> find(String keyId) {
        return jdbc.query("""
                SELECT id,key_id,secret_hash,tenant_id,scopes,active,expires_at
                FROM api_credentials WHERE key_id=?
                """, (rs, row) -> {
            String[] scopes = (String[]) rs.getArray("scopes").getArray();
            Timestamp expires = rs.getTimestamp("expires_at");
            return new ApiCredential(rs.getObject("id", java.util.UUID.class), rs.getString("key_id"),
                    rs.getString("secret_hash"), rs.getString("tenant_id"), new HashSet<>(Arrays.asList(scopes)),
                    rs.getBoolean("active"), expires == null ? null : expires.toInstant());
        }, keyId).stream().findFirst();
    }

    public void recordUse(java.util.UUID credentialId, Instant usedAt) {
        jdbc.update("UPDATE api_credentials SET last_used_at=? WHERE id=?", Timestamp.from(usedAt), credentialId);
    }
}
