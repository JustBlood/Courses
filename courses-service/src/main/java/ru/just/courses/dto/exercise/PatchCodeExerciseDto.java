package ru.just.courses.dto.exercise;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.exercise.CodeExercise;
import ru.just.dtolib.base.Dto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PatchCodeExerciseDto extends Dto<CodeExercise> {
    private Integer ordinalNumber;
    @Size(min = 1, max = 100, message = "number of tests for code should be in bound 1..100")
    private List<CodeExercise.CodeTest> codeTests;


    @Override
    public PatchCodeExerciseDto fromEntity(CodeExercise entity) {
        this.codeTests = entity.getCodeTests();
        this.ordinalNumber = entity.getOrdinalNumber();
        return this;
    }

    @Override
    public CodeExercise toEntity() {
        return new CodeExercise(ordinalNumber, codeTests);
    }
}
