package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.model.GroupMembership;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.GroupMembershipRepository;
import ru.just.monolithmvp.service.StatisticsQueryService.UserCourseKey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsReportService {
    private final StatisticsQueryService statisticsQueryService;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final CsvReportRenderer csvReportRenderer;

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> userCourseStats(Long userId) {
        List<Enrollment> enrollments = statisticsQueryService.findEnrollmentsByUser(userId);
        List<Long> courseIds = distinctIds(enrollments.stream().map(e -> e.getCourse().getId()).toList());
        Map<UserCourseKey, CourseProgress> progressByUserCourse = loadProgressByUserCourse(List.of(userId), courseIds);

        Map<Long, Integer> maxPointsByCourse = statisticsQueryService.sumMaxPointsByCourseIds(courseIds);
        Map<Long, Long> totalLessonsByCourse = statisticsQueryService.countLessonsByCourseIds(courseIds);
        Map<UserCourseKey, Integer> earnedByUserCourse = statisticsQueryService
                .sumEarnedPointsByUserAndCourse(List.of(userId), courseIds);
        Map<UserCourseKey, Long> completedByUserCourse = statisticsQueryService
                .countCompletedLessonsByUserAndCourse(List.of(userId), courseIds);

        return enrollments.stream()
                .map(enrollment -> toStudentCourseStat(enrollment,
                        progressByUserCourse.get(new UserCourseKey(enrollment.getUser().getId(), enrollment.getCourse().getId())),
                        maxPointsByCourse,
                        totalLessonsByCourse,
                        earnedByUserCourse,
                        completedByUserCourse))
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
        Map<UserCourseKey, CourseProgress> progressByUserCourse = loadProgressByUserCourse(userIds, courseIds);

        int maxPoints = statisticsQueryService.sumMaxPointsByCourseIds(courseIds).getOrDefault(courseId, 0);
        long totalLessons = statisticsQueryService.countLessonsByCourseIds(courseIds).getOrDefault(courseId, 0L);
        Map<UserCourseKey, Integer> earnedByUserCourse = statisticsQueryService
                .sumEarnedPointsByUserAndCourse(userIds, courseIds);
        Map<UserCourseKey, Long> completedByUserCourse = statisticsQueryService
                .countCompletedLessonsByUserAndCourse(userIds, courseIds);

        return enrollments.stream()
                .map(enrollment -> {
                    AppUser user = enrollment.getUser();
                    UserCourseKey key = new UserCourseKey(user.getId(), courseId);
                    CourseProgress progressModel = progressByUserCourse.get(key);
                    int earned = earnedByUserCourse.getOrDefault(key, 0);
                    long completed = completedByUserCourse.getOrDefault(key, 0L);
                    double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
                    double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;
                    return new CourseStudentStatDto(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            extractLogin(user.getEmail()),
                            earned,
                            maxPoints,
                            efficiency,
                            progress,
                            completed,
                            totalLessons,
                            fmt(enrollment.getEnrolledAt()),
                            fmt(startedAt(progressModel)),
                            fmt(completedAt(progressModel))
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv() {
        List<Enrollment> enrollments = statisticsQueryService.findAllEnrollments();
        List<Long> userIds = distinctIds(enrollments.stream().map(e -> e.getUser().getId()).toList());
        List<Long> courseIds = distinctIds(enrollments.stream().map(e -> e.getCourse().getId()).toList());
        Map<UserCourseKey, CourseProgress> progressByUserCourse = loadProgressByUserCourse(userIds, courseIds);

        Map<Long, List<GroupMembership>> membershipsByUser = loadMembershipsByUser(userIds);
        Map<Long, Integer> maxPointsByCourse = statisticsQueryService.sumMaxPointsByCourseIds(courseIds);
        Map<UserCourseKey, Integer> earnedByUserCourse = statisticsQueryService
                .sumEarnedPointsByUserAndCourse(userIds, courseIds);

        List<List<String>> rows = enrollments.stream()
                .map(enrollment -> {
                    Long userId = enrollment.getUser().getId();
                    Long courseId = enrollment.getCourse().getId();
                    CourseProgress progress = progressByUserCourse.get(new UserCourseKey(userId, courseId));
                    int maxPoints = maxPointsByCourse.getOrDefault(courseId, 0);
                    int earned = earnedByUserCourse.getOrDefault(new UserCourseKey(userId, courseId), 0);
                    double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
                    String groups = membershipsByUser.getOrDefault(userId, List.of()).stream()
                            .map(membership -> membership.getGroup().getTitle())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.joining(" | "));

                    return List.of(
                            groups,
                            safe(enrollment.getUser().getFullName()),
                            safe(enrollment.getUser().getEmail()),
                            "",
                            "",
                            safe(enrollment.getCourse().getTitle()),
                            "",
                            datePart(enrollment.getEnrolledAt()),
                            timePart(enrollment.getEnrolledAt()),
                            datePart(startedAt(progress)),
                            timePart(startedAt(progress)),
                            datePart(completedAt(progress)),
                            timePart(completedAt(progress)),
                            String.valueOf(earned),
                            String.format(Locale.US, "%.2f", efficiency),
                            "",
                            formatSpentTime(startedAt(progress), completedAt(progress)),
                            "",
                            ""
                    );
                })
                .toList();

        return csvReportRenderer.render(List.of(
                "Группы",
                "ФИО студента",
                "Email",
                "Логин",
                "CID",
                "Название курса",
                "CID",
                "Дата назначения",
                "Время",
                "Дата начала",
                "Время",
                "Дата завершения",
                "Время",
                "Баллов",
                "Эффективность",
                "Продолжительность",
                "Затрачено",
                "Номер сертификата",
                "Ссылка"
        ), rows);
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

        List<Enrollment> enrollments = statisticsQueryService.findEnrollmentsByCourse(courseId);
        List<Long> userIds = distinctIds(enrollments.stream().map(e -> e.getUser().getId()).toList());
        List<Long> courseIds = List.of(courseId);
        Map<UserCourseKey, CourseProgress> progressByUserCourse = loadProgressByUserCourse(userIds, courseIds);

        int maxPoints = statisticsQueryService.sumMaxPointsByCourseIds(courseIds).getOrDefault(courseId, 0);
        long totalLessons = statisticsQueryService.countLessonsByCourseIds(courseIds).getOrDefault(courseId, 0L);
        Map<Long, List<GroupMembership>> membershipsByUser = loadMembershipsByUser(userIds);
        Map<UserCourseKey, Integer> earnedByUserCourse = statisticsQueryService
                .sumEarnedPointsByUserAndCourse(userIds, courseIds);
        Map<UserCourseKey, Long> completedByUserCourse = statisticsQueryService
                .countCompletedLessonsByUserAndCourse(userIds, courseIds);
        Map<UserCourseKey, Integer> retakesByUserCourse = statisticsQueryService
                .sumRetakesByUserAndCourse(userIds, courseIds);

        List<List<String>> rows = enrollments.stream()
                .map(enrollment -> {
                    Long userId = enrollment.getUser().getId();
                    UserCourseKey key = new UserCourseKey(userId, courseId);
                    CourseProgress progressModel = progressByUserCourse.get(key);
                    int earned = earnedByUserCourse.getOrDefault(key, 0);
                    long completed = completedByUserCourse.getOrDefault(key, 0L);
                    double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
                    double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;
                    int retakes = retakesByUserCourse.getOrDefault(key, 0);

                    List<GroupMembership> memberships = membershipsByUser.getOrDefault(userId, List.of());
                    String company = groupTitleByType(memberships, GroupType.COMPANY);
                    String department = groupTitleByType(memberships, GroupType.DEPARTMENT);
                    String position = groupTitleByType(memberships, GroupType.POSITION);
                    String groups = memberships.stream()
                            .map(membership -> membership.getGroup().getTitle())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.joining(" | "));

                    AppUser user = enrollment.getUser();
                    String deactivated = (!user.isActivation() || user.getDeactivatedAt() != null) ? "Да" : "Нет";

                    return List.of(
                            enrollmentStatus(progressModel),
                            "",
                            safe(user.getFullName()),
                            safe(user.getEmail()),
                            "",
                            "",
                            deactivated,
                            company,
                            department,
                            position,
                            groups,
                            String.valueOf(earned),
                            String.format(Locale.US, "%.2f", efficiency),
                            "",
                            String.valueOf(retakes),
                            fmt(enrollment.getEnrolledAt()),
                            fmt(startedAt(progressModel)),
                            fmt(completedAt(progressModel)),
                            calcDeadline(enrollment.getEnrolledAt(), course.getDeadlineDays()),
                            formatSpentTime(startedAt(progressModel), completedAt(progressModel)),
                            String.format(Locale.US, "%.2f%%", progress),
                            completed + "/" + totalLessons,
                            "",
                            "",
                            ""
                    );
                })
                .toList();

        return csvReportRenderer.render(List.of(
                "Статус",
                "Программа",
                "ФИО",
                "Email",
                "Логин",
                "cid",
                "Деактивирован",
                "Компания",
                "Подразделение",
                "Должность",
                "Группы",
                "Баллов",
                "Эффективность",
                "Медалей",
                "Пересдач",
                "Назначено",
                "Начало",
                "Завершение",
                "Дедлайн",
                "Затрачено времени",
                "Прогресс",
                "Уроков",
                "Продолжительность",
                "Номер сертификата",
                "Ссылка"
        ), rows);
    }

    private StudentCourseStatDto toStudentCourseStat(Enrollment enrollment,
                                                     CourseProgress progressModel,
                                                     Map<Long, Integer> maxPointsByCourse,
                                                     Map<Long, Long> totalLessonsByCourse,
                                                     Map<UserCourseKey, Integer> earnedByUserCourse,
                                                     Map<UserCourseKey, Long> completedByUserCourse) {
        Long courseId = enrollment.getCourse().getId();
        Long userId = enrollment.getUser().getId();
        UserCourseKey key = new UserCourseKey(userId, courseId);

        int maxPoints = maxPointsByCourse.getOrDefault(courseId, 0);
        int earned = earnedByUserCourse.getOrDefault(key, 0);
        long completed = completedByUserCourse.getOrDefault(key, 0L);
        long totalLessons = totalLessonsByCourse.getOrDefault(courseId, 0L);

        double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
        double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;

        return new StudentCourseStatDto(
                courseId,
                enrollment.getCourse().getTitle(),
                earned,
                maxPoints,
                efficiency,
                progress,
                completed,
                totalLessons,
                fmt(enrollment.getEnrolledAt()),
                fmt(startedAt(progressModel)),
                fmt(completedAt(progressModel))
        );
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

    private String fmt(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String enrollmentStatus(CourseProgress progress) {
        if (progress != null && progress.getStatus() == CourseProgressStatus.COMPLETED) {
            return "Завершен";
        }
        if (progress != null && (progress.getStartedAt() != null || progress.getStatus() == CourseProgressStatus.IN_PROGRESS)) {
            return "В процессе";
        }
        return "Назначен";
    }

    private LocalDateTime startedAt(CourseProgress progress) {
        return progress == null ? null : progress.getStartedAt();
    }

    private LocalDateTime completedAt(CourseProgress progress) {
        return progress == null ? null : progress.getCompletedAt();
    }

    private String groupTitleByType(List<GroupMembership> memberships, GroupType type) {
        return memberships.stream()
                .filter(membership -> membership.getGroup().getType() == type)
                .map(membership -> membership.getGroup().getTitle())
                .findFirst()
                .orElse("");
    }

    private String calcDeadline(LocalDateTime enrolledAt, Integer deadlineDays) {
        if (enrolledAt == null || deadlineDays == null) {
            return "";
        }
        return fmt(enrolledAt.plusDays(deadlineDays));
    }

    private String formatSpentTime(LocalDateTime startedAt, LocalDateTime completedAt) {
        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) {
            return "";
        }

        Duration duration = Duration.between(startedAt, completedAt);
        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return String.format("%dд %02d:%02d", days, hours, minutes);
        }
        return String.format("%02d:%02d", hours, minutes);
    }

    private String datePart(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.toLocalDate().toString();
    }

    private String timePart(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.toLocalTime().withNano(0).toString();
    }

    private String extractLogin(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
