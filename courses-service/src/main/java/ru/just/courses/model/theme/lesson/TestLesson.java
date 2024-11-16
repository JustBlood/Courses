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
public class TestLesson extends Lesson {
    private String condition;
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "possible_answers", nullable = false)
    private List<String> possibleAnswers;
    private String answer;

    public TestLesson(Integer ordinalNumber, String condition, List<String> possibleAnswers, String answer) {
        super(null, null, ordinalNumber, LessonType.TEST);
        this.condition = condition;
        this.possibleAnswers = possibleAnswers;
        this.answer = answer;
    }
}
