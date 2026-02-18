package ru.just.monolithmvp.dto.program;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.ProgramAccessCondition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record CreateLearningProgramRequest(
        @NotBlank String title,
        String description,
        String coverFilePath,
        @NotNull ProgramAccessCondition accessCondition,
        Duration deadlineAt,
        Boolean blockAfterDeadline,
        @NotEmpty List<@Valid ProgramCourseSettingsRequest> courses
) {
}
