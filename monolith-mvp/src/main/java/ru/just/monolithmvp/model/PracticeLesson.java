package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    private String assignmentPrompt;

    @Column(length = 4000)
    private String optionsRaw;

    @Column(length = 2000)
    private String correctAnswersRaw;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionIndex ASC")
    private List<PracticeQuestion> questions = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void syncLessonType() {
        setLessonType(LessonType.PRACTICE_TEST);
    }
}
