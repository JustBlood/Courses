package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.ResetLessonProgressRequest;
import ru.just.monolithmvp.dto.ResetStudentLessonProgressRequest;
import ru.just.monolithmvp.dto.ResetStudentProgressRequest;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.PendingSubmissionQuestionDto;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.OpenReviewService;
import ru.just.monolithmvp.service.StatisticsService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final OpenReviewService openReviewService;
    private final StatisticsService statisticsService;

    @GetMapping("/reviews/pending")
    @Operation(summary = "Получить pending submissions по open-урокам для ручной проверки", description = "Возвращает список отправленных open-решений, ожидающих проверки")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список submissions для проверки по урокам", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PendingSubmissionDto.class))))
    })
    public ResponseEntity<List<PendingSubmissionDto>> pendingReviews() {
        return ResponseEntity.ok(openReviewService.getPendingReviews());
    }

    @GetMapping("/reviews/pending/{submissionId}")
    @Operation(summary = "Получить вопросы open-урока для детального ревью", description = "Возвращает вопросы и ответы конкретной отправки для ручной проверки")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список вопросов submission для ревью", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PendingSubmissionQuestionDto.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Submission уже финализирован или не является open-уроком", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Submission не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<List<PendingSubmissionQuestionDto>> pendingReviewQuestions(@PathVariable Long submissionId) {
        return ResponseEntity.ok(openReviewService.getPendingReviewQuestions(submissionId));
    }

    @GetMapping("/reviews/courses")
    @Operation(summary = "Получить курсы, назначенные текущему reviewer", description = "Возвращает курсы, по которым текущий reviewer может проверять open-решения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список курсов reviewer", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseSummaryDto.class))))
    })
    public ResponseEntity<List<CourseSummaryDto>> reviewCourses() {
        return ResponseEntity.ok(courseService.getMyReviewerCourseSummaries());
    }

    @PostMapping("/reviews/{submissionId}")
    @Operation(summary = "Сохранить решения ревью по всем вопросам open-урока", description = "Фиксирует решение ревьюера по всем вопросам отправки")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Результат проверки сохранён", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректный статус или payload", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Submission не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> reviewOpenAnswer(@PathVariable Long submissionId,
                                                                @RequestBody ReviewOpenSubmissionRequest request) {
        return ResponseEntity.ok(openReviewService.reviewOpenSubmission(submissionId, request));
    }

    @GetMapping("/courses/{courseId}/stats")
    @Operation(summary = "Получить статистику студентов по курсу", description = "Возвращает агрегированную статистику прохождения по студентам курса")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Статистика по курсу", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseStudentStatDto.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<List<CourseStudentStatDto>> courseStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.courseStats(courseId));
    }

    @PostMapping("/courses/user/reset")
    @Operation(summary = "Сбросить прогресс пользователя по курсу", description = "Удаляет прогресс и отправки выбранного пользователя в выбранном курсе")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Прогресс пользователя по курсу сброшен", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
             @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не записан на курс/курс не существует/пользователь не существует", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> resetStudentProgressByCourse(@RequestBody ResetStudentProgressRequest resetRequest) {
        courseService.resetStudentCourseProgress(resetRequest.userId(), resetRequest.courseId());
        return ResponseEntity.ok(new ApiResponse("Student progress has been cleared"));
    }

    @PostMapping("/lessons/user/reset")
    @Operation(summary = "Сбросить прогресс пользователя по уроку", description = "Удаляет прогресс выбранного пользователя по конкретному уроку")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Прогресс пользователя по уроку сброшен", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не записан на курс/курс не существует/пользователь не существует", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> resetStudentProgressByLesson(@RequestBody ResetStudentLessonProgressRequest resetRequest) {
        courseService.resetStudentLessonProgress(resetRequest.userId(), resetRequest.courseId(), resetRequest.lessonId());
        return ResponseEntity.ok(new ApiResponse("Student progress has been cleared"));
    }

    @PostMapping("/lessons/reset")
    @Operation(summary = "Сбросить прогресс всех пользователей по уроку", description = "Удаляет отправки всех пользователей по конкретному уроку")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Прогресс всех пользователей по уроку сброшен", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не записан на курс/курс не существует/пользователь не существует", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> resetLessonProgressForAllUsers(@RequestBody ResetLessonProgressRequest resetRequest) {
        courseService.resetLessonProgressForAll(resetRequest.courseId(), resetRequest.lessonId());
        return ResponseEntity.ok(new ApiResponse("Lesson progress has been cleared for all users"));
    }

    @GetMapping(value = "/reports/summary.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Скачать общий сводный CSV-отчет по всем курсам", description = "Возвращает CSV-отчет по всем назначениям курсов")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV отчёт", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string")))
    })
    public void summaryCsv(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        statisticsService.writeSummaryReportCsv(response.getWriter());
    }
}
