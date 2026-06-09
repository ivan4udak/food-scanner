package com.foodscanner.api;

import com.foodscanner.api.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Слой: api (тест фильтра корреляции).
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/scan");
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<String> mdcCorr = new AtomicReference<>();
        FilterChain chain = (request, response) -> mdcCorr.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID));

        filter.doFilter(req, res, chain);

        String corr = res.getHeader(CorrelationIdFilter.HEADER);
        assertThat(corr).isNotBlank();
        assertThat(UUID.fromString(corr)).isNotNull(); // валидный UUID
        assertThat(mdcCorr.get()).isEqualTo(corr);
        assertThat(req.getAttribute(CorrelationIdFilter.ATTR_CORRELATION_ID)).isEqualTo(corr);
        assertThat(req.getAttribute(CorrelationIdFilter.ATTR_REQUEST_ID)).isNotNull();
    }

    @Test
    void reusesValidProvidedCorrelationId() throws Exception {
        String provided = UUID.randomUUID().toString();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/scan");
        req.addHeader(CorrelationIdFilter.HEADER, provided);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(res.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(provided);
        assertThat(req.getAttribute(CorrelationIdFilter.ATTR_CORRELATION_ID)).isEqualTo(provided);
    }

    @Test
    void rejectsMalformedCorrelationIdAndGeneratesNew() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/ping");
        req.addHeader(CorrelationIdFilter.HEADER, "not-a-uuid; rm -rf /");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        String corr = res.getHeader(CorrelationIdFilter.HEADER);
        assertThat(corr).isNotEqualTo("not-a-uuid; rm -rf /");
        assertThat(UUID.fromString(corr)).isNotNull();
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/scan");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID)).isNull();
    }
}
