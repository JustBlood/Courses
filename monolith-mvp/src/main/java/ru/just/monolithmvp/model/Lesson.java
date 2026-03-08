package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_course_position", columnNames = {"course_id", "position"})
        }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "lesson_kind", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType lessonType;

    @Column(nullable = false)
    private Integer fullPoints = 1;

    @Column(nullable = false)
    private Integer partialPoints = 0;

    @Column(nullable = false)
    private Boolean stopLesson = false;

    @Column(nullable = false)
    private Boolean blockedDuringAttempt = true;

    @Column
    private Integer attemptLimit;

    @Column
    private Integer timeLimitMinutes;
}
