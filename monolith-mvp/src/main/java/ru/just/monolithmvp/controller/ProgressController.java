package ru.just.monolithmvp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
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
public class ProgressController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<PendingSubmissionDto>> pendingReviews() {
        return ResponseEntity.ok(learningService.getPendingReviews());
    }

    @GetMapping("/reviews/courses")
    public ResponseEntity<List<CourseSummaryDto>> reviewCourses() {
        return ResponseEntity.ok(courseService.getMyReviewerCourseSummaries());
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

    @GetMapping(value = "/courses/{courseId}/summary-report.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> courseSummaryCsv(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.summaryReportCsv(courseId));
    }
}
