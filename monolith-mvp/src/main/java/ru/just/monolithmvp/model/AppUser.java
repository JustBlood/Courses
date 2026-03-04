package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean activation = true;

    @Column(name = "activated", nullable = false)
    private boolean enabled = true;

    private String phone;

    @Column(length = 2000)
    private String comment;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastVisit;
    private LocalDateTime deactivatedAt;
    private String deactivatedBy;
    private String avatarFilePath;
}
