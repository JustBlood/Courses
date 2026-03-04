package ru.just.monolithmvp.service;

public interface EmailService {
    void sendPasswordLink(String toEmail, String fullName, String link);
}
