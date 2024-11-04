package ru.just.courses.dto.exercise;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.exercise.MultiTestExercise;
import ru.just.dtolib.base.Dto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PatchMultiTestExerciseDto extends Dto<MultiTestExercise> {
    private Integer ordinalNumber;
    private List<String> possibleAnswers;
    private List<String> correctAnswers;

    @Override
    public PatchMultiTestExerciseDto fromEntity(MultiTestExercise entity) {
        this.ordinalNumber = entity.getOrdinalNumber();
        this.possibleAnswers = entity.getPossibleAnswers();
        this.correctAnswers = entity.getCorrectAnswers();
        return this;
    }

    @Override
    public MultiTestExercise toEntity() {
        return new MultiTestExercise();
    }
}
