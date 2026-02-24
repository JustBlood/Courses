package ru.just.monolithmvp.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BusinessEventLogger {

    private static final Logger log = LoggerFactory.getLogger(BusinessEventLogger.class);

    private final ObservabilityMetricsService metricsService;

    public BusinessEventLogger(ObservabilityMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public void log(String event, String outcome, Object... keyValues) {
        StringBuilder builder = new StringBuilder();
        builder.append("event=").append(event).append(" outcome=").append(outcome);

        if (keyValues != null) {
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                builder.append(' ')
                        .append(String.valueOf(keyValues[i]))
                        .append('=')
                        .append(String.valueOf(keyValues[i + 1]));
            }
        }

        log.info(builder.toString());
        metricsService.incrementBusinessEvent(event, outcome);
    }
}
