package ru.just.progressservice.model.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "user_lesson_progress")
public class UserLessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private UserThemeProgress themeProgress;

    @Column(nullable = false)
    private Long lessonId;

    @Column
    private Integer ordinalNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column
    private ZonedDateTime completedAt;

    @Column
    private String userAnswer;

    @Column
    private String verificationResult;
}

