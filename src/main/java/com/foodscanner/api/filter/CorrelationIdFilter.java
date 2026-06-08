package com.foodscanner.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Слой: api (HTTP-инфраструктура).
 *
 * Связывает клиентскую операцию и серверный запрос в единую диагностическую цепочку:
 *  - читает X-Correlation-Id из запроса (если валидный UUID) либо генерирует новый;
 *  - генерирует requestId на конкретный HTTP-запрос;
 *  - кладёт оба в request-атрибуты и в MDC (для структурированных логов);
 *  - возвращает X-Correlation-Id в ответе;
 *  - гарантированно очищает MDC после запроса.
 *
 * Выполняется максимально рано, чтобы correlationId был доступен всем логам запроса.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    public static final String ATTR_CORRELATION_ID = "correlationId";
    public static final String ATTR_REQUEST_ID     = "requestId";

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID     = "requestId";

    /** Принимаем только UUID-подобные значения — защита от header-инъекций и мусора. */
    private static final Pattern UUID_RE =
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = sanitizeOrGenerate(request.getHeader(HEADER));
        String requestId = UUID.randomUUID().toString();

        request.setAttribute(ATTR_CORRELATION_ID, correlationId);
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private static String sanitizeOrGenerate(String provided) {
        if (provided != null && UUID_RE.matcher(provided).matches()) {
            return provided;
        }
        return UUID.randomUUID().toString();
    }
}
