package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    @Column(name = "options_raw", length = 4000)
    @Convert(converter = StringListJsonConverter.class)
    private List<String> options;

    @Column(name = "correct_answers_raw", length = 2000)
    @Convert(converter = StringListJsonConverter.class)
    private List<String> correctAnswers;

    @Column(length = 4000)
    private String trainerHint;

    @Column(nullable = false)
    private Integer fullPoints = 1;

    @Column(nullable = false)
    private Integer partialPoints = 0;
}
