package ru.just.monolithmvp.dto.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.ProgramAccessCondition;

import java.util.HashSet;
import java.util.List;

public record CreateLearningProgramRequest(
        @NotBlank String title,
        String description,
        ProgramAccessCondition accessCondition,
        Long deadlineAt,
        Boolean blockAfterDeadline,
        @NotEmpty List<Long> courses
) {
    public CreateLearningProgramRequest(@NotBlank String title, String description, ProgramAccessCondition accessCondition, Long deadlineAt, Boolean blockAfterDeadline, @NotEmpty List<Long> courses) {
        this.title = title;
        this.description = description;
        this.accessCondition = accessCondition;
        this.deadlineAt = deadlineAt;
        this.blockAfterDeadline = blockAfterDeadline;
        if (new HashSet<>(courses).size() != courses.size()) {
            throw new BadRequestException("Courses must be unique");
        }
        this.courses = courses;
    }
}
