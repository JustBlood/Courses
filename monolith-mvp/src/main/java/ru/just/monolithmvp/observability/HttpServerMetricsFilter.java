package ru.just.monolithmvp.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
public class HttpServerMetricsFilter extends OncePerRequestFilter {

    private static final String START_NANOS_ATTR = HttpServerMetricsFilter.class.getName() + ".startNanos";

    private final ObservabilityMetricsService metricsService;

    public HttpServerMetricsFilter(ObservabilityMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(START_NANOS_ATTR, System.nanoTime());
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object started = request.getAttribute(START_NANOS_ATTR);
            long startNanos = started instanceof Long ? (Long) started : System.nanoTime();
            long durationNanos = Math.max(0L, System.nanoTime() - startNanos);

            String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (uri == null || uri.isBlank()) {
                uri = request.getRequestURI();
            }

            metricsService.recordHttp(request.getMethod(), uri, response.getStatus(), durationNanos);
        }
    }
}
