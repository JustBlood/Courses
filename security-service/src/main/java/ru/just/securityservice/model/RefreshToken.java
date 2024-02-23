package ru.just.securityservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "refresh_token", uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "user_id"}))
public class RefreshToken {
    @Id @Column(nullable = false)
    private UUID id;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private UUID deviceId;
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    private User user;
}
