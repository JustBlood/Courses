package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PRACTICE")
@Getter
@Setter
@NoArgsConstructor
public class PracticeLesson extends Lesson {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(length = 4000, nullable = false)
    private String questionText;

    @Column(length = 4000)
    private String optionsRaw;

    @Column(length = 2000)
    private String correctAnswersRaw;

    @PrePersist
    @PreUpdate
    private void syncLessonType() {
        setLessonType(LessonType.PRACTICE);
    }
}
