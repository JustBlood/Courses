package ru.just.progressservice.model.user;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "user_module_progress")
public class UserModuleProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private UserCourseProgress courseProgress;

    @Column(nullable = false)
    private Long moduleId;

    @Column
    private Integer ordinalNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer completedThemes = 0;

    @Column(nullable = false)
    private Integer totalThemes;

    @OneToMany(mappedBy = "moduleProgress")
    private List<UserThemeProgress> themeProgresses;

    @Column(nullable = false)
    private Boolean completed = false;
}

