package ru.just.courses.dto;

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
    private Integer ordinalNumber;
    private List<String> possibleAnswers;
    private String answer;

    @Override
    public Dto<TestExercise> fromEntity(TestExercise entity) {
        possibleAnswers = entity.getPossibleAnswers();
        ordinalNumber = entity.getOrdinalNumber();
        answer = entity.getAnswer();
        return this;
    }

    @Override
    public TestExercise toEntity() { // todo: доделать упражнения
        return new TestExercise(possibleAnswers, answer);
    }
}
