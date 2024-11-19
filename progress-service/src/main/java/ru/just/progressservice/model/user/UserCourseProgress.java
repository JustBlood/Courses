package ru.just.progressservice.model.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "user_course_progress")
public class UserCourseProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Integer completedLessons = 0;

    @Column(nullable = false)
    private Integer totalLessons;

    @Column(nullable = false)
    private Boolean active = true;

    @Column
    private ZonedDateTime completedAt;

    @Column
    private ZonedDateTime createdAt;

    @OneToMany(mappedBy = "courseProgress")
    private List<UserModuleProgress> moduleProgresses;
}

