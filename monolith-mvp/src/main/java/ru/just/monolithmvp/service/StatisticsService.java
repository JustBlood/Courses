package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.stat.ReportRowDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> myCourseStats() {
        Long userId = securityUtils.currentUserId();
        return enrollmentRepository.findByUserId(userId).stream()
                .map(e -> toStudentCourseStat(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseStudentStatDto> courseStats(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

        int maxPoints = maxPoints(courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);

        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> {
                    int earned = earnedPoints(e.getUser().getId(), courseId);
                    long completed = submissionRepository.countDistinctPassedLessons(e.getUser().getId(), courseId);
                    double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
                    return new CourseStudentStatDto(
                            e.getUser().getId(),
                            e.getUser().getFullName(),
                            e.getUser().getEmail(),
                            extractLogin(e.getUser().getEmail()),
                            earned,
                            maxPoints,
                            efficiency,
                            completed,
                            totalLessons,
                            fmt(e.getEnrolledAt()),
                            fmt(e.getStartedAt()),
                            fmt(e.getCompletedAt())
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv() {
        List<ReportRowDto> rows = new ArrayList<>();
        for (Enrollment e : enrollmentRepository.findAll()) {
            Long courseId = e.getCourse().getId();
            int maxPoints = maxPoints(courseId);
            int earned = earnedPoints(e.getUser().getId(), courseId);
            double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;

            rows.add(new ReportRowDto(
                    e.getUser().getFullName(),
                    e.getUser().getEmail(),
                    extractLogin(e.getUser().getEmail()),
                    "",
                    e.getCourse().getTitle(),
                    earned,
                    maxPoints,
                    efficiency,
                    fmt(e.getEnrolledAt()),
                    fmt(e.getStartedAt()),
                    fmt(e.getCompletedAt())
            ));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("fullName,email,login,lang,course,earnedPoints,maxPoints,efficiency,enrolledAt,startedAt,completedAt\n");
        for (ReportRowDto row : rows) {
            csv.append(escape(row.fullName())).append(',')
                    .append(escape(row.email())).append(',')
                    .append(escape(row.login())).append(',')
                    .append(escape(row.lang())).append(',')
                    .append(escape(row.courseTitle())).append(',')
                    .append(row.earnedPoints()).append(',')
                    .append(row.maxPoints()).append(',')
                    .append(String.format(Locale.US, "%.2f", row.efficiencyPercent())).append(',')
                    .append(escape(row.enrolledAt())).append(',')
                    .append(escape(row.startedAt())).append(',')
                    .append(escape(row.completedAt()))
                    .append('\n');
        }
        return csv.toString();
    }

    private StudentCourseStatDto toStudentCourseStat(Enrollment e, Long userId) {
        Long courseId = e.getCourse().getId();
        int maxPoints = maxPoints(courseId);
        int earned = earnedPoints(userId, courseId);
        long completed = submissionRepository.countDistinctPassedLessons(userId, courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);
        double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;

        return new StudentCourseStatDto(
                courseId,
                e.getCourse().getTitle(),
                earned,
                maxPoints,
                efficiency,
                completed,
                totalLessons,
                fmt(e.getEnrolledAt()),
                fmt(e.getStartedAt()),
                fmt(e.getCompletedAt())
        );
    }

    private int maxPoints(Long courseId) {
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .mapToInt(l -> Optional.ofNullable(l.getFullPoints()).orElse(0))
                .sum();
    }

    private int earnedPoints(Long studentId, Long courseId) {
        List<LessonSubmission> submissions = submissionRepository.findByStudentIdAndLessonCourseId(studentId, courseId);
        Map<Long, Integer> bestByLesson = new HashMap<>();
        for (LessonSubmission s : submissions) {
            Long lessonId = s.getLesson().getId();
            int points = Optional.ofNullable(s.getPointsAwarded()).orElse(0);
            bestByLesson.merge(lessonId, points, Math::max);
        }
        return bestByLesson.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String fmt(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private String extractLogin(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
