package ru.just.courses.dto.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.exercise.TestExercise;
import ru.just.dtolib.base.Dto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TestExerciseDto extends Dto<TestExercise> {
    @NotNull(message = "unique ordinal number in theme for exercise should be specified")
    private Integer ordinalNumber;
    @NotNull(message = "possible answers should be specified")
    @Size(min = 2, message = "possible answers size should be greater then 1")
    private List<String> possibleAnswers;
    @NotBlank(message = "answer should be specified")
    private String answer;

    @Override
    public TestExerciseDto fromEntity(TestExercise entity) {
        possibleAnswers = entity.getPossibleAnswers();
        ordinalNumber = entity.getOrdinalNumber();
        answer = entity.getAnswer();
        return this;
    }

    @Override
    public TestExercise toEntity() {
        return new TestExercise(ordinalNumber, possibleAnswers, answer);
    }
}
