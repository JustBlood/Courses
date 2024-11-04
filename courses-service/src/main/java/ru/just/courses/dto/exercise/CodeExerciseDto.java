package ru.just.courses.dto.exercise;

import jakarta.validation.constraints.NotNull;
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
public class CodeExerciseDto extends Dto<CodeExercise> {
    @NotNull(message = "unique ordinal number in theme for exercise should be specified")
    private Integer ordinalNumber;
    @NotNull(message = "possible answers should be specified")
    @Size(min = 1, max = 100, message = "number of tests for code should be in bound 1..100")
    private List<CodeExercise.CodeTest> codeTests;

    @Override
    public CodeExerciseDto fromEntity(CodeExercise entity) {
        this.ordinalNumber = entity.getOrdinalNumber();
        this.codeTests = entity.getCodeTests();
        return this;
    }

    @Override
    public CodeExercise toEntity() {
        return new CodeExercise(ordinalNumber, codeTests);
    }
}
