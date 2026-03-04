package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_program_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_program_assignment", columnNames = {"group_id", "program_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GroupProgramAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private LearningGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private LearningProgram program;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
