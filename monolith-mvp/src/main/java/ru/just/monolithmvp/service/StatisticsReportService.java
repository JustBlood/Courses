package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.stat.ReportRowDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.service.StatisticsQueryService.UserCourseKey;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsReportService {
    private static final int CSV_FLUSH_EVERY_ROWS = 50;

    private final StatisticsQueryService statisticsQueryService;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final CsvReportRenderer csvReportRenderer;
    private final PracticeScoringPolicy practiceScoringPolicy;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final StatisticsReportFormatter formatter;

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> userCourseStats(Long userId) {
        List<Enrollment> enrollments = statisticsQueryService.findEnrollmentsByUser(userId);
        List<Long> courseIds = distinctIds(enrollments.stream().map(e -> e.getCourse().getId()).toList());
        ReportContext context = buildReportContext(
                List.of(userId),
                courseIds,
                false,
                true,
                false
        );

        return enrollments.stream()
                .map(enrollment -> toStudentCourseStat(enrollment, context))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseStudentStatDto> courseStats(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new NotFoundException("Course not found: " + courseId);
        }

        List<Enrollment> enrollments = statisticsQueryService.findEnrollmentsByCourse(courseId);
        List<Long> userIds = distinctIds(enrollments.stream().map(e -> e.getUser().getId()).toList());
        List<Long> courseIds = List.of(courseId);

        ReportContext context = buildReportContext(
                userIds,
                courseIds,
                false,
                true,
                false
        );

        long totalLessons = context.totalLessonsByCourse().getOrDefault(courseId, 0L);
        int maxPoints = context.maxPointsByCourse().getOrDefault(courseId, 0);

        return enrollments.stream()
                .map(enrollment -> {
                    AppUser user = enrollment.getUser();
                    UserCourseKey key = new UserCourseKey(user.getId(), courseId);
                    CourseProgress progressModel = context.progressByUserCourse().get(key);
                    long completed = context.completedByUserCourse().getOrDefault(key, 0L);
                    int progress = totalLessons == 0
                            ? 0
                            : formatter.roundToInt(((double) completed * 100D) / totalLessons);

                    return new CourseStudentStatDto(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            formatter.extractLogin(user.getEmail()),
                            context.earnedByUserCourse().getOrDefault(key, 0),
                            maxPoints,
                            context.efficiencyByUserCourse().getOrDefault(key, 0),
                            progress,
                            completed,
                            totalLessons,
                            progressModel == null ? null : progressModel.getStatus(),
                            formatter.fmt(enrollment.getEnrolledAt()),
                            formatter.fmt(formatter.startedAt(progressModel)),
                            formatter.fmt(formatter.completedAt(progressModel))
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public void writeSummaryReportCsv(Writer writer, LocalDateTime from, LocalDateTime to) throws IOException {
        List<Enrollment> enrollments = statisticsQueryService.findAllCompletedEnrollments(from, to);
        List<Long> userIds = distinctIds(enrollments.stream().map(e -> e.getUser().getId()).toList());
        List<Long> courseIds = distinctIds(enrollments.stream().map(e -> e.getCourse().getId()).toList());

        ReportContext context = buildReportContext(
                userIds,
                courseIds,
                true,
                false,
                false
        );

        List<String> header = List.of(
                "ФИО студента",
                "СНИЛС",
                "ID курса",
                "Название курса",
                "Дата назначения",
                "Дата начала",
                "Дата завершения",
                "Получено баллов",
                "Максимум баллов",
                "Эффективность",
                "Затрачено"
        );
        csvReportRenderer.writeHeader(writer, header);

        List<SummaryCsvRow> rows = enrollments.stream()
                .map(enrollment -> toSummaryCsvRow(enrollment, context))
                .toList();

        writeCsvRows(writer, rows, this::toSummaryCsvColumns);
    }

    private StudentCourseStatDto toStudentCourseStat(Enrollment enrollment, ReportContext context) {
        Long courseId = enrollment.getCourse().getId();
        Long userId = enrollment.getUser().getId();
        UserCourseKey key = new UserCourseKey(userId, courseId);
        long completed = context.completedByUserCourse().getOrDefault(key, 0L);
        long totalLessons = context.totalLessonsByCourse().getOrDefault(courseId, 0L);
        int progress = totalLessons == 0
                ? 0
                : formatter.roundToInt(((double) completed * 100D) / totalLessons);

        CourseProgress progressModel = context.progressByUserCourse().get(key);

        return new StudentCourseStatDto(
                courseId,
                enrollment.getCourse().getTitle(),
                context.earnedByUserCourse().getOrDefault(key, 0),
                context.maxPointsByCourse().getOrDefault(courseId, 0),
                context.efficiencyByUserCourse().getOrDefault(key, 0),
                progress,
                completed,
                totalLessons,
                formatter.fmt(enrollment.getEnrolledAt()),
                formatter.fmt(formatter.startedAt(progressModel)),
                formatter.fmt(formatter.completedAt(progressModel))
        );
    }

    private SummaryCsvRow toSummaryCsvRow(Enrollment enrollment, ReportContext context) {
        Long userId = enrollment.getUser().getId();
        Long courseId = enrollment.getCourse().getId();
        UserCourseKey key = new UserCourseKey(userId, courseId);
        CourseProgress progress = context.progressByUserCourse().get(key);

        ReportRowDto row = new ReportRowDto(
                enrollment.getUser().getFullName(),
                enrollment.getUser().getSnils(),
                courseId,
                enrollment.getCourse().getTitle(),
                context.earnedByUserCourse().getOrDefault(key, 0),
                context.maxPointsByCourse().get(courseId),
                context.efficiencyByUserCourse().getOrDefault(key, 0),
                enrollment.getEnrolledAt(),
                formatter.startedAt(progress),
                formatter.completedAt(progress)
        );

        return new SummaryCsvRow(
                row,
                formatter.formatSpentTime(row.startedAt(), row.completedAt())
        );
    }

    private List<String> toSummaryCsvColumns(SummaryCsvRow row) {
        ReportRowDto base = row.base();
        return List.of(
                formatter.safe(base.fullName()),
                formatter.safe(base.snils()),
                String.valueOf(base.courseId()),
                formatter.safe(base.courseTitle()),
                formatter.datePart(base.enrolledAt()),
                formatter.datePart(base.startedAt()),
                formatter.datePart(base.completedAt()),
                String.valueOf(base.earnedPoints()),
                String.valueOf(base.maxPoints()),
                String.valueOf(base.efficiencyPercent()),
                row.spentTime()
        );
    }

    private <T> void writeCsvRows(Writer writer,
                                  List<T> rows,
                                  java.util.function.Function<T, List<String>> rowMapper) throws IOException {
        int rowCount = 0;
        for (T row : rows) {
            csvReportRenderer.writeRow(writer, rowMapper.apply(row));
            rowCount++;
            if (rowCount % CSV_FLUSH_EVERY_ROWS == 0) {
                writer.flush();
            }
        }
        writer.flush();
    }

    private ReportContext buildReportContext(Collection<Long> userIds,
                                             Collection<Long> courseIds,
                                             boolean includeMemberships,
                                             boolean includeCompleted,
                                             boolean includeRetakes) {
        List<Long> normalizedUserIds = distinctIds(userIds);
        List<Long> normalizedCourseIds = distinctIds(courseIds);

        Map<UserCourseKey, CourseProgress> progressByUserCourse = loadProgressByUserCourse(normalizedUserIds, normalizedCourseIds);
        UserCoursePointsAggregation pointsAggregation = aggregatePointsByUserAndCourse(normalizedUserIds, normalizedCourseIds);

        Map<UserCourseKey, Integer> earnedByUserCourse = pointsAggregation.earnedPointsByUserCourse();
        Map<UserCourseKey, Integer> efficiencyByUserCourse = calculateEfficiencyByUserAndCourse(
                pointsAggregation.earnedCompletedPointsByUserCourse(),
                pointsAggregation.maxCompletedLessonPointsByUserCourse()
        );

        return new ReportContext(
                progressByUserCourse,
                statisticsQueryService.sumMaxPointsByCourseIds(normalizedCourseIds),
                statisticsQueryService.countLessonsByCourseIds(normalizedCourseIds),
                earnedByUserCourse,
                efficiencyByUserCourse,
                includeCompleted
                        ? statisticsQueryService.countCompletedLessonsByUserAndCourse(normalizedUserIds, normalizedCourseIds)
                        : Map.of(),
                includeRetakes
                        ? statisticsQueryService.sumRetakesByUserAndCourse(normalizedUserIds, normalizedCourseIds)
                        : Map.of(),
                includeMemberships
                        ? loadMembershipsByUser(normalizedUserIds)
                        : Map.of()
        );
    }

    private UserCoursePointsAggregation aggregatePointsByUserAndCourse(Collection<Long> userIds,
                                                                       Collection<Long> courseIds) {
        List<Long> normalizedUserIds = distinctIds(userIds);
        List<Long> normalizedCourseIds = distinctIds(courseIds);
        if (normalizedUserIds.isEmpty() || normalizedCourseIds.isEmpty()) {
            return new UserCoursePointsAggregation(Map.of(), Map.of(), Map.of());
        }

        List<LessonSubmission> submissions = lessonSubmissionRepository
                .findByStudentIdInAndLessonCourseIdInAndStatusIn(normalizedUserIds, normalizedCourseIds, SubmissionStatus.FINAL_STATUSES);
        if (submissions.isEmpty()) {
            return new UserCoursePointsAggregation(Map.of(), Map.of(), Map.of());
        }

        List<Long> practiceLessonIds = submissions.stream()
                .map(LessonSubmission::getLesson)
                .filter(lesson -> lesson.getLessonType().isPractice())
                .map(Lesson::getId)
                .distinct()
                .toList();

        Map<Long, Map<Long, PracticeQuestion>> questionsByLessonId = loadPracticeQuestionsByLessonId(practiceLessonIds);

        Map<UserCourseKey, Integer> earnedByUserCourse = new java.util.HashMap<>();
        Map<UserCourseKey, Integer> earnedCompletedByUserCourse = new java.util.HashMap<>();
        Map<UserCourseKey, Integer> maxCompletedByUserCourse = new java.util.HashMap<>();
        for (LessonSubmission submission : submissions) {
            Long userId = submission.getStudent().getId();
            Long courseId = submission.getLesson().getCourse().getId();
            int submissionPoints = calculateSubmissionPoints(
                    submission,
                    questionsByLessonId.getOrDefault(submission.getLesson().getId(), Map.of())
            );

            UserCourseKey key = new UserCourseKey(userId, courseId);
            earnedByUserCourse.merge(key, submissionPoints, Integer::sum);
            earnedCompletedByUserCourse.merge(key, submissionPoints, Integer::sum);
            maxCompletedByUserCourse.merge(key, Optional.ofNullable(submission.getLesson().getFullPoints()).orElse(0), Integer::sum);
        }

        return new UserCoursePointsAggregation(earnedByUserCourse, earnedCompletedByUserCourse, maxCompletedByUserCourse);
    }

    private Map<UserCourseKey, Integer> calculateEfficiencyByUserAndCourse(Map<UserCourseKey, Integer> earnedCompletedByUserCourse,
                                                                            Map<UserCourseKey, Integer> maxCompletedByUserCourse) {
        Map<UserCourseKey, Integer> efficiencyByUserCourse = new java.util.HashMap<>();
        for (Map.Entry<UserCourseKey, Integer> maxCompletedEntry : maxCompletedByUserCourse.entrySet()) {
            UserCourseKey key = maxCompletedEntry.getKey();
            int maxCompletedPoints = maxCompletedEntry.getValue();
            int earnedCompletedPoints = earnedCompletedByUserCourse.getOrDefault(key, 0);
            int efficiency = maxCompletedPoints == 0
                    ? 0
                    : formatter.roundToInt(((double) earnedCompletedPoints * 100D) / maxCompletedPoints);
            efficiencyByUserCourse.put(key, efficiency);
        }
        return efficiencyByUserCourse;
    }

    private int calculateSubmissionPoints(LessonSubmission submission, Map<Long, PracticeQuestion> practiceQuestionsById) {
        Lesson lesson = submission.getLesson();
        if (!lesson.getLessonType().isPractice()) {
            return lesson.getFullPoints();
        }

        return Optional.ofNullable(submission.getQuestionProgress())
                .orElse(List.of())
                .stream()
                .filter(progress -> progress.getPointsType() != null)
                .mapToInt(progress -> {
                    PracticeQuestion question = practiceQuestionsById.get(progress.getQuestionId());
                    return question == null ? 0 : practiceScoringPolicy.scoreQuestion(progress.getPointsType(), question);
                })
                .sum();
    }

    private Map<Long, Map<Long, PracticeQuestion>> loadPracticeQuestionsByLessonId(Collection<Long> lessonIds) {
        List<Long> normalizedLessonIds = distinctIds(lessonIds);
        if (normalizedLessonIds.isEmpty()) {
            return Map.of();
        }

        return practiceQuestionRepository.findByLessonIdIn(normalizedLessonIds).stream()
                .collect(Collectors.groupingBy(
                        question -> question.getLesson().getId(),
                        Collectors.toMap(
                                PracticeQuestion::getId,
                                question -> question,
                                (left, right) -> left
                        )
                ));
    }

    private Map<UserCourseKey, CourseProgress> loadProgressByUserCourse(Collection<Long> userIds,
                                                                        Collection<Long> courseIds) {
        List<Long> normalizedUserIds = distinctIds(userIds);
        List<Long> normalizedCourseIds = distinctIds(courseIds);
        if (normalizedUserIds.isEmpty() || normalizedCourseIds.isEmpty()) {
            return Map.of();
        }

        return courseProgressRepository.findByUserIdInAndCourseIdIn(normalizedUserIds, normalizedCourseIds).stream()
                .collect(Collectors.toMap(
                        cp -> new UserCourseKey(cp.getUser().getId(), cp.getCourse().getId()),
                        cp -> cp,
                        (left, right) -> left
                ));
    }

    private Map<Long, List<GroupMembership>> loadMembershipsByUser(Collection<Long> userIds) {
        List<Long> ids = distinctIds(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        return groupMembershipRepository.findByUserIdInWithGroup(ids).stream()
                .collect(Collectors.groupingBy(membership -> membership.getUser().getId()));
    }

    private List<Long> distinctIds(Collection<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record SummaryCsvRow(ReportRowDto base,
                                 String spentTime) {
    }

    private record UserCoursePointsAggregation(Map<UserCourseKey, Integer> earnedPointsByUserCourse,
                                               Map<UserCourseKey, Integer> earnedCompletedPointsByUserCourse,
                                               Map<UserCourseKey, Integer> maxCompletedLessonPointsByUserCourse) {
    }

    private record ReportContext(Map<UserCourseKey, CourseProgress> progressByUserCourse,
                                 Map<Long, Integer> maxPointsByCourse,
                                 Map<Long, Long> totalLessonsByCourse,
                                 Map<UserCourseKey, Integer> earnedByUserCourse,
                                 Map<UserCourseKey, Integer> efficiencyByUserCourse,
                                 Map<UserCourseKey, Long> completedByUserCourse,
                                 Map<UserCourseKey, Integer> retakesByUserCourse,
                                 Map<Long, List<GroupMembership>> membershipsByUser) {
    }
}
