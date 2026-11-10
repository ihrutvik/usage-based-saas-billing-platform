package dev.hrutvik.billing.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTest {
    private static final Instant NOW=Instant.parse("2026-11-10T12:00:00Z");
    private static final String HASH="cd577fe2561ebff23505db0bb006300c7cdecbd46bc0e03c449afafaca2c25bf";

    @Test void derivesTenantAndAllowsRequiredScope() throws Exception {
        var store=mock(ApiCredentialStore.class); var id=UUID.randomUUID();
        when(store.find("demo")).thenReturn(Optional.of(new ApiCredential(id,"demo",HASH,"tenant-a",Set.of("usage:write"),true,null)));
        var request=request("POST","/v1/usage-events","bk_demo.demo-secret"); request.addHeader("X-Tenant-Id","spoofed"); var response=new MockHttpServletResponse();
        new ApiKeyAuthenticationFilter(store,Clock.fixed(NOW,ZoneOffset.UTC)).doFilter(request,response,(req,res)->assertEquals("tenant-a",((jakarta.servlet.http.HttpServletRequest)req).getHeader("X-Tenant-Id")));
        assertEquals(200,response.getStatus()); verify(store).recordUse(id,NOW);
    }

    @Test void rejectsUnknownOrMalformedKey() throws Exception {
        var store=mock(ApiCredentialStore.class); var response=new MockHttpServletResponse();
        new ApiKeyAuthenticationFilter(store,Clock.fixed(NOW,ZoneOffset.UTC)).doFilter(request("GET","/v1/usage-summary","bad"),response,(a,b)->fail());
        assertEquals(401,response.getStatus()); verifyNoInteractions(store);
    }

    @Test void rejectsMissingScope() throws Exception {
        var store=mock(ApiCredentialStore.class);
        when(store.find("demo")).thenReturn(Optional.of(new ApiCredential(UUID.randomUUID(),"demo",HASH,"tenant",Set.of("usage:read"),true,null)));
        var response=new MockHttpServletResponse();
        new ApiKeyAuthenticationFilter(store,Clock.fixed(NOW,ZoneOffset.UTC)).doFilter(request("POST","/v1/invoices/finalize","bk_demo.demo-secret"),response,(a,b)->fail());
        assertEquals(403,response.getStatus()); assertTrue(response.getContentAsString().contains("billing:write"));
    }

    @Test void rejectsExpiredCredential() throws Exception {
        var store=mock(ApiCredentialStore.class);
        when(store.find("demo")).thenReturn(Optional.of(new ApiCredential(UUID.randomUUID(),"demo",HASH,"tenant",Set.of("usage:read"),true,NOW.minusSeconds(1))));
        var response=new MockHttpServletResponse();
        new ApiKeyAuthenticationFilter(store,Clock.fixed(NOW,ZoneOffset.UTC)).doFilter(request("GET","/v1/usage-summary","bk_demo.demo-secret"),response,(a,b)->fail());
        assertEquals(401,response.getStatus());
    }

    private MockHttpServletRequest request(String method,String path,String key){var request=new MockHttpServletRequest(method,path);request.addHeader("X-API-Key",key);return request;}
}
