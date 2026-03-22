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
    @Column(nullable = false)
    private Integer passingThresholdPercent = 100;

    @Column(nullable = false)
    private Boolean shuffleOnEveryAttempt = false;

    @Column(nullable = false)
    private Boolean showQuestionStatus = true;

    @Column(nullable = false)
    private Boolean showCorrectAnswersAfterCompletion = false;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionIndex ASC")
    private List<PracticeQuestion> questions = new ArrayList<>();

    public boolean passedByPoints(Integer awardedPoints) {
        return awardedPoints * 100 >= getFullPoints() * passingThresholdPercent;
    }
}
