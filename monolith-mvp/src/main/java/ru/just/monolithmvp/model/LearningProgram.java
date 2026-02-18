package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_programs")
@Getter
@Setter
@NoArgsConstructor
public class LearningProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column
    private String coverFilePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgramAccessCondition accessCondition = ProgramAccessCondition.PREVIOUS_COURSES_COMPLETED;

    private LocalDateTime deadlineAt;

    @Column(nullable = false)
    private Boolean blockAfterDeadline = false;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ProgramCourse> courses = new ArrayList<>();
}
