package ru.just.courses.model.theme.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.controller.config.StringListAttributeConverter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"theme_id", "ordinal_number"})})
public class MultiTestLesson extends Lesson {
    private String condition;
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "possible_answers", nullable = false)
    private List<String> possibleAnswers;
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "correctAnswers", nullable = false)
    private List<String> correctAnswers;

    public MultiTestLesson(Integer ordinalNumber, List<String> possibleAnswers, List<String> correctAnswers) {
        super(null, null, ordinalNumber, LessonType.MULTI_TEST);
        this.possibleAnswers = possibleAnswers;
        this.correctAnswers = correctAnswers;
    }
}
