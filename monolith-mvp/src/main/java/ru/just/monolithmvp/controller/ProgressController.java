package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.CreatePracticeLessonRequest;
import ru.just.monolithmvp.dto.lesson.CreateTheoryLessonRequest;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.LearningService;
import ru.just.monolithmvp.service.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProgressController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;

    @PostMapping("/courses/{courseId}/lessons/theory")
    public ResponseEntity<LessonDto> createTheoryLesson(@PathVariable Long courseId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return new ResponseEntity<>(courseService.createTheoryLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/courses/{courseId}/lessons/practice")
    public ResponseEntity<LessonDto> createPracticeLesson(@PathVariable Long courseId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return new ResponseEntity<>(courseService.createPracticeLesson(courseId, request), HttpStatus.CREATED);
    }

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<PendingSubmissionDto>> pendingReviews() {
        return ResponseEntity.ok(learningService.getPendingReviews());
    }

    @PostMapping("/reviews/{submissionId}")
    public ResponseEntity<SubmissionResultDto> reviewOpenAnswer(@PathVariable Long submissionId,
                                                                @RequestBody ReviewOpenSubmissionRequest request) {
        return ResponseEntity.ok(learningService.reviewOpenSubmission(submissionId, request));
    }

    @GetMapping("/courses/{courseId}/stats")
    public ResponseEntity<List<CourseStudentStatDto>> courseStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.courseStats(courseId));
    }

    @GetMapping(value = "/reports/summary.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> summaryCsv() {
        return ResponseEntity.ok(statisticsService.summaryReportCsv());
    }
}
