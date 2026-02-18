package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "program_courses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_program_course", columnNames = {"program_id", "course_id"}),
                @UniqueConstraint(name = "uk_program_order", columnNames = {"program_id", "order_index"})
        })
@Getter
@Setter
@NoArgsConstructor
public class ProgramCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private LearningProgram program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}