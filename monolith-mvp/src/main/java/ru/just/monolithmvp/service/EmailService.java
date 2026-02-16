package ru.just.monolithmvp.service;

public interface EmailService {
    void sendInvite(String toEmail, String fullName, String link);
}
