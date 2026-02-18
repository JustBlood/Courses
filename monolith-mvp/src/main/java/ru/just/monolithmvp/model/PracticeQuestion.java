package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "practice_questions")
@Getter
@Setter
@NoArgsConstructor
public class PracticeQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private PracticeLesson lesson;

    @Column(nullable = false)
    private Integer questionIndex;

    @Enumerated(EnumType.STRING)
    @Column
    private QuestionType questionType;

    @Column(length = 4000, nullable = false)
    private String questionText;

    @Column(length = 4000)
    private String optionsRaw;

    @Column(length = 2000)
    private String correctAnswersRaw;

    @Column(length = 4000)
    private String trainerHint;

    @Column(nullable = false)
    private Integer fullPoints = 1;

    @Column(nullable = false)
    private Integer partialPoints = 0;
}