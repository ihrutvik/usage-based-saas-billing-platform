package dev.hrutvik.billing.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test void preservesSafeClientCorrelationIdDuringRequest() throws Exception {
        var request = new MockHttpServletRequest(); request.addHeader(CorrelationIdFilter.HEADER, "billing-job_42");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("billing-job_42"));
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("billing-job_42");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test void replacesUnsafeCorrelationIdAndNeverReflectsHeaderInjection() throws Exception {
        var request = new MockHttpServletRequest(); request.addHeader(CorrelationIdFilter.HEADER, "bad value with spaces");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).matches("[0-9a-f-]{36}"));
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test void createsCorrelationIdWhenHeaderIsMissing() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {});
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }
}
