package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.config.properties.MailProperties;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.observability.ObservabilityMetricsService;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
public class YandexSmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(YandexSmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ObservabilityMetricsService metricsService;
    private final BusinessEventLogger businessEventLogger;

    @Override
    public void sendPasswordLink(String toEmail, String fullName, String link) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(toEmail);
            message.setSubject("Cистема обучения Shels");
            message.setText("Здравствуйте, " + fullName + "!\n\n" +
                    "Для установки пароля перейдите по ссылке:\n" + link + "\n");
            mailSender.send(message);
            metricsService.incrementEmail("yandex-smtp", "success");
            businessEventLogger.log("email.password_link.send", "success", "provider", "yandex-smtp", "to", toEmail);
            log.info("Invite email sent to {}", toEmail);
        } catch (RuntimeException ex) {
            metricsService.incrementEmail("yandex-smtp", "failure");
            businessEventLogger.log("email.password_link.send", "failure", "provider", "yandex-smtp", "to", toEmail, "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
