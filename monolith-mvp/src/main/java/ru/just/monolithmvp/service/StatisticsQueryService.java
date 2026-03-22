package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.projection.CourseMetricProjection;
import ru.just.monolithmvp.repository.projection.UserCourseMetricProjection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsQueryService {
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public List<Enrollment> findEnrollmentsByUser(Long userId) {
        return enrollmentRepository.findByUserIdWithUserAndCourse(userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseIdWithUserAndCourse(courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findAllCompletedEnrollments() {
        return enrollmentRepository.findAllWithUserAndCourseByProgressStatus(CourseProgressStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> sumMaxPointsByCourseIds(Collection<Long> courseIds) {
        List<Long> normalizedCourseIds = normalizeIds(courseIds);
        if (normalizedCourseIds.isEmpty()) {
            return Map.of();
        }

        return lessonRepository.sumFullPointsByCourseIds(normalizedCourseIds).stream()
                .collect(Collectors.toMap(
                        CourseMetricProjection::getCourseId,
                        row -> row.getValue().intValue()
                ));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countLessonsByCourseIds(Collection<Long> courseIds) {
        List<Long> normalizedCourseIds = normalizeIds(courseIds);
        if (normalizedCourseIds.isEmpty()) {
            return Map.of();
        }

        return lessonRepository.countLessonsByCourseIds(normalizedCourseIds).stream()
                .collect(Collectors.toMap(
                        CourseMetricProjection::getCourseId,
                        CourseMetricProjection::getValue
                ));
    }

    @Transactional(readOnly = true)
    public Map<UserCourseKey, Long> countCompletedLessonsByUserAndCourse(Collection<Long> userIds,
                                                                          Collection<Long> courseIds) {
        return aggregateUserCourseMetric(userIds, courseIds,
                ids -> submissionRepository.countCompletedLessonsByUserIdsAndCourseIds(ids.userIds(), ids.courseIds()),
                Function.identity());
    }

    @Transactional(readOnly = true)
    public Map<UserCourseKey, Integer> sumRetakesByUserAndCourse(Collection<Long> userIds,
                                                                  Collection<Long> courseIds) {
        return aggregateUserCourseMetric(userIds, courseIds,
                ids -> submissionRepository.sumRetakesByUserIdsAndCourseIds(ids.userIds(), ids.courseIds()),
                value -> value.intValue());
    }

    private <T> Map<UserCourseKey, T> aggregateUserCourseMetric(Collection<Long> userIds,
                                                                Collection<Long> courseIds,
                                                                Function<IdsPair, List<UserCourseMetricProjection>> loader,
                                                                Function<Long, T> valueMapper) {
        List<Long> normalizedUserIds = normalizeIds(userIds);
        List<Long> normalizedCourseIds = normalizeIds(courseIds);
        if (normalizedUserIds.isEmpty() || normalizedCourseIds.isEmpty()) {
            return Map.of();
        }

        return loader.apply(new IdsPair(normalizedUserIds, normalizedCourseIds)).stream()
                .collect(Collectors.toMap(
                        row -> new UserCourseKey(row.getUserId(), row.getCourseId()),
                        row -> valueMapper.apply(row.getValue())
                ));
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record IdsPair(List<Long> userIds, List<Long> courseIds) {
    }

    public record UserCourseKey(Long userId, Long courseId) {
    }
}
