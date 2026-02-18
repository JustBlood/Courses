package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "program_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_program_enrollment_user_program", columnNames = {"user_id", "program_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ProgramEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private LearningProgram program;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;
}