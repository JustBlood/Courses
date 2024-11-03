package ru.just.courses.model.theme.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.controller.config.StringListAttributeConverter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TestExercise extends Exercise {
    @Convert(converter = StringListAttributeConverter.class)
    @Column(name = "possible_answers", nullable = false)
    private List<String> possibleAnswers;
    private String answer;
}
