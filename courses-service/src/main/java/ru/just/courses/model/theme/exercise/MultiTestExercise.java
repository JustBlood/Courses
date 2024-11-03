package ru.just.courses.model.theme.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import ru.just.courses.controller.config.StringListAttributeConverter;

import java.util.List;

@Entity
public class MultiTestExercise extends Exercise {
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "possible_answers", nullable = false)
    private List<String> possibleAnswers;
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "correctAnswers", nullable = false)
    private List<String> correctAnswers;
}
