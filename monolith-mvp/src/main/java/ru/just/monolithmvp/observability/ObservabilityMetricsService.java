package ru.just.monolithmvp.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityMetricsService {

    private final MeterRegistry meterRegistry;

    public ObservabilityMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordHttp(String method, String uri, int status, long durationNanos) {
        String safeMethod = method == null ? "UNKNOWN" : method;
        String safeUri = uri == null || uri.isBlank() ? "UNKNOWN" : uri;
        String statusTag = String.valueOf(status);
        String outcome = status >= 500 ? "SERVER_ERROR" : status >= 400 ? "CLIENT_ERROR" : "SUCCESS";

        Timer.builder("courses_http_server_requests_seconds")
                .tag("method", safeMethod)
                .tag("uri", safeUri)
                .tag("status", statusTag)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        meterRegistry.counter("courses_http_requests_total",
                "method", safeMethod,
                "uri", safeUri,
                "status", statusTag,
                "outcome", outcome).increment();

        if (status >= 400) {
            meterRegistry.counter("courses_http_errors_total",
                    "method", safeMethod,
                    "uri", safeUri,
                    "status", statusTag).increment();
        }
    }

    public void incrementAuth(String operation, String outcome) {
        meterRegistry.counter("courses_auth_attempts_total",
                "operation", operation,
                "outcome", outcome).increment();
    }

    public void incrementEmail(String provider, String outcome) {
        meterRegistry.counter("courses_email_send_total",
                "provider", provider,
                "outcome", outcome).increment();
    }

    public void incrementBusinessEvent(String event, String outcome) {
        meterRegistry.counter("courses_business_events_total",
                "event", event,
                "outcome", outcome).increment();
    }
}
