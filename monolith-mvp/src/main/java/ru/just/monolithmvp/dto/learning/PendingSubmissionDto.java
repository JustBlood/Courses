package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Submission урока, доступный ревьюверу в списке pending")
public record PendingSubmissionDto(
        @Schema(description = "ID submission", example = "1")
        Long submissionId,
        @Schema(description = "ID урока", example = "1")
        Long lessonId,
        @Schema(description = "Название урока", example = "Урок №1")
        String lessonTitle,
        @Schema(description = "ID курса", example = "1")
        Long courseId,
        @Schema(description = "Название курса", example = "Курс такой-то")
        String courseTitle,
        @Schema(description = "ID студента", example = "1")
        Long studentId,
        @Schema(description = "ФИО студента", example = "Зубенко Михаил Петрович")
        String studentFullname,
        @Schema(description = "Дата отправки submission", example = "1095292800")
        Long submittedAt,
        @Schema(description = "Номер попытки", example = "1")
        Integer attempt
) {
}
