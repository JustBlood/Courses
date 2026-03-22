package ru.just.monolithmvp.dto.program;

import jakarta.validation.constraints.NotBlank;
import ru.just.monolithmvp.model.ProgramAccessCondition;

public record CreateLearningProgramRequest(
        @NotBlank String title,
        String description,
        ProgramAccessCondition accessCondition,
        Long deadlineDays,
        Boolean blockAfterDeadline
) {
}
