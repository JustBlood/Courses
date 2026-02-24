package ru.just.monolithmvp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.observability.ObservabilityMetricsService;

@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class NoopEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(NoopEmailService.class);

    private final ObservabilityMetricsService metricsService;
    private final BusinessEventLogger businessEventLogger;

    public NoopEmailService(ObservabilityMetricsService metricsService, BusinessEventLogger businessEventLogger) {
        this.metricsService = metricsService;
        this.businessEventLogger = businessEventLogger;
    }

    @Override
    public void sendPasswordLink(String toEmail, String fullName, String link) {
        metricsService.incrementEmail("noop", "failure");
        businessEventLogger.log("email.password_link.send", "failure", "provider", "noop", "to", toEmail, "reason", "mail_sender_not_configured");
        log.warn("Mail sender is not configured. Invite was NOT sent. to={}, fullName={}, link={}", toEmail, fullName, link);
    }
}
