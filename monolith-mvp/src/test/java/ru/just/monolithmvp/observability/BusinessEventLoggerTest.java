package ru.just.monolithmvp.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessEventLoggerTest {

    @Test
    void should_redact_sensitive_values_in_log_message() {
        ObservabilityMetricsService metricsService = new ObservabilityMetricsService(new SimpleMeterRegistry());
        BusinessEventLogger logger = new BusinessEventLogger(metricsService);

        Logger targetLogger = (Logger) LoggerFactory.getLogger(BusinessEventLogger.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        targetLogger.addAppender(appender);
        try {
            logger.log(
                    "auth.login",
                    "failure",
                    "email", "admin@local",
                    "token", "jwt-token-value",
                    "secret", "super-secret",
                    "reason", "BadCredentialsException"
            );

            assertThat(appender.list).isNotEmpty();
            String message = appender.list.getLast().getFormattedMessage();

            assertThat(message)
                    .contains("event=auth.login")
                    .contains("outcome=failure")
                    .contains("email=[REDACTED]")
                    .contains("token=[REDACTED]")
                    .contains("secret=[REDACTED]")
                    .contains("reason=BadCredentialsException")
                    .doesNotContain("admin@local")
                    .doesNotContain("jwt-token-value")
                    .doesNotContain("super-secret");
        } finally {
            targetLogger.detachAppender(appender);
        }
    }
}
