package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_submission_status_history")
@Getter
@Setter
@NoArgsConstructor
public class LessonSubmissionStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private LessonSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private SubmissionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private SubmissionStatus toStatus;

    @Column(name = "changed_by_admin_id")
    private Long changedByAdminId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 2000)
    private String comment;
}