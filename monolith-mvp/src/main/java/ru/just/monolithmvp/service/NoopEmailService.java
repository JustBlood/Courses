package ru.just.monolithmvp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class NoopEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(NoopEmailService.class);

    @Override
    public void sendInvite(String toEmail, String fullName, String link) {
        log.warn("Mail sender is not configured. Invite was NOT sent. to={}, fullName={}, link={}", toEmail, fullName, link);
    }
}
