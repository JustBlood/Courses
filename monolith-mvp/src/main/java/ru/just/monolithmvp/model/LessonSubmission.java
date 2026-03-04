package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_submissions")
@Getter
@Setter
@NoArgsConstructor
public class LessonSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(length = 4000)
    private String answerRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(nullable = false)
    private Boolean passed;

    @Column(nullable = false)
    private Integer pointsAwarded = 0;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private Long reviewedByAdminId;
    private LocalDateTime reviewedAt;
    @Column(length = 2000)
    private String reviewComment;
}
