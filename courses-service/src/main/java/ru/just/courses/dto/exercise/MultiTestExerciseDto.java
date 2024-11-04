package ru.just.courses.dto.exercise;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.exercise.MultiTestExercise;
import ru.just.dtolib.base.Dto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MultiTestExerciseDto extends Dto<MultiTestExercise> {
    @NotNull(message = "unique ordinal number in theme for exercise should be specified")
    private Integer ordinalNumber;
    @NotNull(message = "possible answers should be specified")
    @Size(min = 2, max = 10, message = "possible answers size should be in bound 2..10")
    private List<String> possibleAnswers;
    @NotNull(message = "possible answers should be specified")
    @Size(max = 10)
    private List<String> correctAnswers;

    @Override
    public MultiTestExerciseDto fromEntity(MultiTestExercise entity) {
        this.ordinalNumber = entity.getOrdinalNumber();
        this.possibleAnswers = entity.getPossibleAnswers();
        this.correctAnswers = entity.getCorrectAnswers();
        return this;
    }

    @Override
    public MultiTestExercise toEntity() {
        return new MultiTestExercise(ordinalNumber, possibleAnswers, correctAnswers);
    }
}
