package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 255)
    private String authorFullName;

    @Column
    private String coverFilePath;

    @Column(nullable = false)
    private Integer passingThresholdPercent = 70;

    @Column
    private Integer deadlineDays;

    @Column(nullable = false)
    private Boolean lessonsFreeOrder = false;

    @Column(nullable = false)
    private Boolean allowContinueAfterFail = false;

    @Column(nullable = false)
    private Boolean keepAccessAfterDeadline = false;

    @Column(nullable = false)
    private Boolean includeInOverallStats = true;

    @Column(nullable = false)
    private Long createdByAdminId;

    private LocalDateTime deadlineAt;

    @Column(nullable = false)
    private Boolean blockAfterDeadline = false;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Lesson> lessons = new ArrayList<>();
}
