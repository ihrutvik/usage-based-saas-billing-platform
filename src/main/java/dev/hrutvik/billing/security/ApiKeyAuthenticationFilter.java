package dev.hrutvik.billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    static final String API_KEY_HEADER = "X-API-Key";
    static final String TENANT_HEADER = "X-Tenant-Id";
    private final ApiCredentialStore credentials;
    private final Clock clock;

    public ApiKeyAuthenticationFilter(ApiCredentialStore credentials) { this(credentials, Clock.systemUTC()); }
    ApiKeyAuthenticationFilter(ApiCredentialStore credentials, Clock clock) { this.credentials = credentials; this.clock = clock; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        ParsedKey parsed = ParsedKey.parse(request.getHeader(API_KEY_HEADER));
        if (parsed == null) { reject(response, 401, "INVALID_API_KEY", "A valid X-API-Key is required"); return; }
        var credential = credentials.find(parsed.keyId()).orElse(null);
        Instant now = clock.instant();
        if (credential == null || !credential.isUsable(now) || !matches(parsed.secret(), credential.secretHash())) {
            reject(response, 401, "INVALID_API_KEY", "A valid X-API-Key is required"); return;
        }
        String required = requiredScope(request.getMethod(), request.getRequestURI());
        if (required != null && !credential.scopes().contains(required)) {
            reject(response, 403, "INSUFFICIENT_SCOPE", "Required scope: " + required); return;
        }
        credentials.recordUse(credential.id(), now);
        chain.doFilter(new TenantRequest(request, credential.tenantId()), response);
    }

    private boolean matches(String secret, String expectedHash) {
        try {
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().parseHex(expectedHash);
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception invalidHash) { return false; }
    }

    private String requiredScope(String method, String path) {
        if (path.equals("/v1/usage-events")) return "usage:write";
        if (path.startsWith("/v1/usage-summary") || path.startsWith("/v1/realtime-usage") || path.startsWith("/v1/rating-previews")) return "usage:read";
        if (path.startsWith("/v1/billing-adjustments") && method.equals("GET")) return "billing:read";
        if (path.startsWith("/v1/invoices") || path.startsWith("/v1/billing-adjustments")) return "billing:write";
        return null;
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    private record ParsedKey(String keyId, String secret) {
        static ParsedKey parse(String value) {
            if (value == null || !value.startsWith("bk_") || value.length() > 300) return null;
            int separator = value.indexOf('.', 3);
            if (separator < 4 || separator == value.length() - 1) return null;
            String keyId = value.substring(3, separator), secret = value.substring(separator + 1);
            return keyId.matches("[A-Za-z0-9_-]{1,64}") && secret.length() >= 8 ? new ParsedKey(keyId, secret) : null;
        }
    }

    private static final class TenantRequest extends HttpServletRequestWrapper {
        private final String tenantId;
        TenantRequest(HttpServletRequest request, String tenantId) { super(request); this.tenantId = tenantId; }
        @Override public String getHeader(String name) { return TENANT_HEADER.equalsIgnoreCase(name) ? tenantId : super.getHeader(name); }
        @Override public Enumeration<String> getHeaders(String name) { return TENANT_HEADER.equalsIgnoreCase(name) ? Collections.enumeration(java.util.List.of(tenantId)) : super.getHeaders(name); }
    }
}
