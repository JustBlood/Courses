package ru.just.progressservice.model.user;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "user_theme_progress")
public class UserThemeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private UserModuleProgress moduleProgress;

    @Column(nullable = false)
    private Long themeId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer completedLessons = 0;

    @Column(nullable = false)
    private Integer totalLessons;

    @OneToMany(mappedBy = "themeProgress")
    private List<UserLessonProgress> lessonProgresses;

    @Column(nullable = false)
    private Boolean completed = false;
}

