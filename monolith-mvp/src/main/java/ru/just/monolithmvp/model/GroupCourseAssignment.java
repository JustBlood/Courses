package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_course_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_course_assignment", columnNames = {"group_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GroupCourseAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private LearningGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
