package ru.just.securityservice.config.token.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Token (UUID id, String subject, UUID deviceId, List<String> authorities, Instant createdAt, Instant expiresAt) {}
