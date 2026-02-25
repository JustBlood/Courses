package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.model.GroupMembership;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.ProgramEnrollment;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.GroupMembershipRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.ProgramCourseRepository;
import ru.just.monolithmvp.repository.ProgramEnrollmentRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> myCourseStats() {
        Long userId = securityUtils.currentUserId();
        return userCourseStats(userId);
    }

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> userCourseStats(Long userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(e -> toStudentCourseStat(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseStudentStatDto> courseStats(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new NotFoundException("Course not found: " + courseId);
        }

        int maxPoints = maxPoints(courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);

        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> {
                    int earned = earnedPoints(e.getUser().getId(), courseId);
                    long completed = submissionRepository.countDistinctPassedLessons(e.getUser().getId(), courseId);
                    double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
                    double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;
                    return new CourseStudentStatDto(
                            e.getUser().getId(),
                            e.getUser().getFullName(),
                            e.getUser().getEmail(),
                            extractLogin(e.getUser().getEmail()),
                            earned,
                            maxPoints,
                            efficiency,
                            progress,
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
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        List<Long> userIds = enrollments.stream()
                .map(e -> e.getUser().getId())
                .distinct()
                .toList();

        Map<Long, List<GroupMembership>> membershipsByUser = groupMembershipRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(m -> m.getUser().getId()));

        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, List.of(
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
        ));

        for (Enrollment enrollment : enrollments) {
            Long userId = enrollment.getUser().getId();
            Long courseId = enrollment.getCourse().getId();

            int maxPoints = maxPoints(courseId);
            int earned = earnedPoints(userId, courseId);
            double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;

            String groups = membershipsByUser.getOrDefault(userId, List.of()).stream()
                    .map(m -> m.getGroup().getTitle())
                    .distinct()
                    .collect(Collectors.joining(" | "));

            appendCsvRow(csv, List.of(
                    groups,
                    safe(enrollment.getUser().getFullName()),
                    safe(enrollment.getUser().getEmail()),
                    "",
                    "",
                    safe(enrollment.getCourse().getTitle()),
                    "",
                    datePart(enrollment.getEnrolledAt()),
                    timePart(enrollment.getEnrolledAt()),
                    datePart(enrollment.getStartedAt()),
                    timePart(enrollment.getStartedAt()),
                    datePart(enrollment.getCompletedAt()),
                    timePart(enrollment.getCompletedAt()),
                    String.valueOf(earned),
                    String.format(Locale.US, "%.2f", efficiency),
                    "",
                    formatSpentTime(enrollment.getStartedAt(), enrollment.getCompletedAt()),
                    "",
                    ""
            ));
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv(Long courseId) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        int maxPoints = maxPoints(courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);

        List<Long> userIds = enrollments.stream()
                .map(e -> e.getUser().getId())
                .toList();

        Map<Long, List<GroupMembership>> membershipsByUser = groupMembershipRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(m -> m.getUser().getId()));

        Set<Long> courseProgramIds = programCourseRepository.findByCourseId(courseId).stream()
                .map(pc -> pc.getProgram().getId())
                .collect(Collectors.toSet());

        Map<Long, List<String>> programsByUser = new HashMap<>();
        for (ProgramEnrollment enrollment : programEnrollmentRepository.findByUserIdIn(userIds)) {
            Long programId = enrollment.getProgram().getId();
            if (!courseProgramIds.contains(programId)) {
                continue;
            }
            programsByUser
                    .computeIfAbsent(enrollment.getUser().getId(), __ -> new ArrayList<>())
                    .add(enrollment.getProgram().getTitle());
        }

        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, List.of(
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
        ));

        for (Enrollment enrollment : enrollments) {
            Long userId = enrollment.getUser().getId();
            int earned = earnedPoints(userId, courseId);
            long completed = submissionRepository.countDistinctPassedLessons(userId, courseId);
            double efficiency = maxPoints == 0 ? 0D : ((double) earned * 100D) / maxPoints;
            double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;

            List<GroupMembership> memberships = membershipsByUser.getOrDefault(userId, List.of());

            String program = String.join(" | ", programsByUser.getOrDefault(userId, List.of()));
            String company = groupTitleByType(memberships, GroupType.COMPANY);
            String department = groupTitleByType(memberships, GroupType.DEPARTMENT);
            String position = groupTitleByType(memberships, GroupType.POSITION);
            String groups = memberships.stream()
                    .map(m -> m.getGroup().getTitle())
                    .distinct()
                    .collect(Collectors.joining(" | "));

            int retakes = retakes(userId, courseId);
            String spentTime = formatSpentTime(enrollment.getStartedAt(), enrollment.getCompletedAt());
            String progressText = String.format(Locale.US, "%.2f%%", progress);
            String lessons = completed + "/" + totalLessons;
            String deadline = calcDeadline(enrollment.getEnrolledAt(), course.getDeadlineDays());

            AppUser user = enrollment.getUser();
            String deactivated = (!user.isActivation() || user.getDeactivatedAt() != null) ? "Да" : "Нет";

            appendCsvRow(csv, List.of(
                    enrollmentStatus(enrollment),
                    program,
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
                    fmt(enrollment.getStartedAt()),
                    fmt(enrollment.getCompletedAt()),
                    deadline,
                    spentTime,
                    progressText,
                    lessons,
                    "",
                    "",
                    ""
            ));
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
        double progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons;

        return new StudentCourseStatDto(
                courseId,
                e.getCourse().getTitle(),
                earned,
                maxPoints,
                efficiency,
                progress,
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String enrollmentStatus(Enrollment enrollment) {
        if (enrollment.getCompletedAt() != null) {
            return "Завершен";
        }
        if (enrollment.getStartedAt() != null) {
            return "В процессе";
        }
        return "Назначен";
    }

    private String groupTitleByType(List<GroupMembership> memberships, GroupType type) {
        return memberships.stream()
                .filter(m -> m.getGroup().getType() == type)
                .map(m -> m.getGroup().getTitle())
                .findFirst()
                .orElse("");
    }

    private int retakes(Long studentId, Long courseId) {
        Map<Long, Long> attemptsByLesson = submissionRepository.findByStudentIdAndLessonCourseId(studentId, courseId).stream()
                .collect(Collectors.groupingBy(s -> s.getLesson().getId(), Collectors.counting()));

        return attemptsByLesson.values().stream()
                .mapToInt(count -> (int) Math.max(0L, count - 1L))
                .sum();
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

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
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
