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
    @Column
    private QuestionType questionType;

    @Column(length = 4000)
    private String questionText;

    @Column(length = 4000)
    private String optionsRaw;

    @Column(length = 2000)
    private String correctAnswersRaw;

    @Column(length = 4000)
    private String assignmentPrompt;

    @PrePersist
    @PreUpdate
    private void syncLessonType() {
        if (questionType == null) {
            setLessonType(LessonType.PRACTICE_ASSIGNMENT);
        } else {
            setLessonType(LessonType.PRACTICE_TEST);
        }
    }
}
