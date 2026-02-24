package ru.just.monolithmvp.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class BusinessEventLogger {

    private static final Logger log = LoggerFactory.getLogger(BusinessEventLogger.class);

    private final ObservabilityMetricsService metricsService;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "email",
            "to",
            "password",
            "token",
            "secret",
            "authorization",
            "link"
    );

    public BusinessEventLogger(ObservabilityMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public void log(String event, String outcome, Object... keyValues) {
        StringBuilder builder = new StringBuilder();
        builder.append("event=").append(event).append(" outcome=").append(outcome);

        if (keyValues != null) {
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                String key = String.valueOf(keyValues[i]);
                builder.append(' ')
                        .append(key)
                        .append('=')
                        .append(sanitizeValue(key, keyValues[i + 1]));
            }
        }

        log.info(builder.toString());
        metricsService.incrementBusinessEvent(event, outcome);
    }

    private String sanitizeValue(String key, Object value) {
        if (value == null) {
            return "null";
        }
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (SENSITIVE_KEYS.contains(normalized)) {
            return "[REDACTED]";
        }
        return String.valueOf(value);
    }
}
