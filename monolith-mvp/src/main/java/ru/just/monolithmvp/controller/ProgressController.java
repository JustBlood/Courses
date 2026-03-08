package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.PendingSubmissionQuestionDto;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.LearningService;
import ru.just.monolithmvp.service.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Progress & Reports", description = "Проверка open-ended ответов, статистика и отчеты")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class ProgressController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;

    @GetMapping("/reviews/pending")
    @Operation(summary = "Получить pending submissions по open-урокам для ручной проверки")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список submissions для проверки по урокам", content = @Content(schema = @Schema(implementation = PendingSubmissionDto.class)))
    })
    public ResponseEntity<List<PendingSubmissionDto>> pendingReviews() {
        return ResponseEntity.ok(learningService.getPendingReviews());
    }

    @GetMapping("/reviews/pending/{submissionId}")
    @Operation(summary = "Получить вопросы open-урока для детального ревью")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список вопросов submission для ревью", content = @Content(schema = @Schema(implementation = PendingSubmissionQuestionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Submission уже финализирован или не является open-уроком", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Submission не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<List<PendingSubmissionQuestionDto>> pendingReviewQuestions(@PathVariable Long submissionId) {
        return ResponseEntity.ok(learningService.getPendingReviewQuestions(submissionId));
    }

    @GetMapping("/reviews/courses")
    @Operation(summary = "Получить курсы, назначенные текущему reviewer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список курсов reviewer", content = @Content(schema = @Schema(implementation = CourseSummaryDto.class)))
    })
    public ResponseEntity<List<CourseSummaryDto>> reviewCourses() {
        return ResponseEntity.ok(courseService.getMyReviewerCourseSummaries());
    }

    @PostMapping("/reviews/{submissionId}")
    @Operation(summary = "Сохранить решения ревью по всем вопросам open-урока")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Результат проверки сохранён", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректный статус или payload", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Submission не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> reviewOpenAnswer(@PathVariable Long submissionId,
                                                                @RequestBody ReviewOpenSubmissionRequest request) {
        return ResponseEntity.ok(learningService.reviewOpenSubmission(submissionId, request));
    }

    @GetMapping("/courses/{courseId}/stats")
    @Operation(summary = "Получить статистику студентов по курсу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Статистика по курсу", content = @Content(schema = @Schema(implementation = CourseStudentStatDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<List<CourseStudentStatDto>> courseStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.courseStats(courseId));
    }

    @GetMapping(value = "/reports/summary.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Скачать общий сводный CSV-отчет по всем курсам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV отчёт", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string")))
    })
    public ResponseEntity<String> summaryCsv() {
        return ResponseEntity.ok(statisticsService.summaryReportCsv());
    }

    @GetMapping(value = "/courses/{courseId}/summary-report.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Скачать сводный CSV-отчет по конкретному курсу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV отчёт по курсу", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<String> courseSummaryCsv(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.summaryReportCsv(courseId));
    }
}
