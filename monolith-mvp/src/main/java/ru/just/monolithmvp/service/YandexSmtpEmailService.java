package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.config.properties.MailProperties;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
public class YandexSmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(YandexSmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendInvite(String toEmail, String fullName, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(toEmail);
        message.setSubject("Приглашение в систему обучения");
        message.setText("Здравствуйте, " + fullName + "!\n\n" +
                "Для установки пароля перейдите по ссылке:\n" + link + "\n");
        mailSender.send(message);
        log.info("Invite email sent to {}", toEmail);
    }
}
