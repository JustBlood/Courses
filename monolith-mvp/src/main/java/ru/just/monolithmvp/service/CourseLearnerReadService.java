package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.course.CourseLearnerDto;
import ru.just.monolithmvp.dto.lesson.LearnerLessonSummaryDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearnerReadService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public CourseLearnerDto getCourseForLearner(Long userId, Long courseId) {
        courseAccessPolicy.assertStudentEnrolled(userId, courseId);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, courseId);
        Course course = getCourseEntity(courseId);
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
        Map<Long, LessonSubmission> submissionsByLessonId = loadSubmissionsByLessonId(userId, courseId);
        Set<Long> passedLessonIds = resolvePassedLessonIds(submissionsByLessonId);
        List<LearnerLessonSummaryDto> lessons = buildLearnerLessonSummaries(course, courseLessons, submissionsByLessonId, passedLessonIds);

        return toCourseLearnerDto(course, lessons);
    }

    @Transactional(readOnly = true)
    public Long findNextLessonIdForLearner(Long userId, Long courseId) {
        courseAccessPolicy.assertStudentEnrolled(userId, courseId);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, courseId);

        Course course = getCourseEntity(courseId);
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
        Set<Long> passedLessonIds = resolvePassedLessonIds(loadSubmissionsByLessonId(userId, courseId));

        boolean hasUnpassedStopBefore = false;
        for (int index = 0; index < courseLessons.size(); index++) {
            Lesson lesson = courseLessons.get(index);
            boolean passed = passedLessonIds.contains(lesson.getId());
            if (passed) {
                continue;
            }

            boolean blockedByPreviousLesson = Boolean.FALSE.equals(course.getLessonsFreeOrder())
                    && index > 0
                    && !passedLessonIds.contains(courseLessons.get(index - 1).getId());
            boolean blockedByStopLesson = hasUnpassedStopBefore;
            if (!blockedByPreviousLesson && !blockedByStopLesson) {
                return lesson.getId();
            }

            if (Boolean.TRUE.equals(lesson.getStopLesson())) {
                hasUnpassedStopBefore = true;
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public Course getCourseEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    private Map<Long, LessonSubmission> loadSubmissionsByLessonId(Long userId, Long courseId) {
        return submissionRepository.findByStudentIdAndLessonCourseId(userId, courseId).stream()
                .collect(Collectors.toMap(
                        submission -> submission.getLesson().getId(),
                        submission -> submission,
                        (left, right) -> right
                ));
    }

    private Set<Long> resolvePassedLessonIds(Map<Long, LessonSubmission> submissionsByLessonId) {
        return submissionsByLessonId.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue().getCompleted()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private List<LearnerLessonSummaryDto> buildLearnerLessonSummaries(Course course,
                                                                       List<Lesson> courseLessons,
                                                                       Map<Long, LessonSubmission> submissionsByLessonId,
                                                                       Set<Long> passedLessonIds) {
        List<LearnerLessonSummaryDto> lessons = new ArrayList<>(courseLessons.size());
        boolean hasUnpassedStopBefore = false;

        for (int index = 0; index < courseLessons.size(); index++) {
            Lesson lesson = courseLessons.get(index);
            boolean passed = passedLessonIds.contains(lesson.getId());

            String blockReason = resolveLessonBlockReason(course, courseLessons, index, passedLessonIds, hasUnpassedStopBefore);
            boolean blocked = blockReason != null;
            Integer pointsAwarded = Optional.ofNullable(submissionsByLessonId.get(lesson.getId()))
                    .map(LessonSubmission::getPointsAwarded)
                    .orElse(0);

            lessons.add(new LearnerLessonSummaryDto(
                    lesson.getId(),
                    lesson.getPosition(),
                    lesson.getTitle(),
                    lesson.getLessonType(),
                    passed,
                    pointsAwarded,
                    blocked,
                    blockReason
            ));

            if (Boolean.TRUE.equals(lesson.getStopLesson()) && !passed) {
                hasUnpassedStopBefore = true;
            }
        }

        return lessons;
    }

    private CourseLearnerDto toCourseLearnerDto(Course course, List<LearnerLessonSummaryDto> lessons) {
        int totalLessons = lessons.size();
        int completedLessons = (int) lessons.stream().filter(LearnerLessonSummaryDto::passed).count();
        int remainingLessons = Math.max(totalLessons - completedLessons, 0);
        int completionPercent = totalLessons == 0 ? 100 : (completedLessons * 100) / totalLessons;
        boolean courseCompleted = completedLessons >= totalLessons;

        return new CourseLearnerDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                fileStorageService.normalizeStoredPath(course.getCoverFilePath()),
                course.getDeadlineDays(),
                completionPercent,
                completedLessons,
                remainingLessons,
                totalLessons,
                courseCompleted,
                lessons
        );
    }

    private String resolveLessonBlockReason(Course course,
                                            List<Lesson> courseLessons,
                                            int lessonIndex,
                                            Set<Long> passedLessonIds,
                                            boolean hasUnpassedStopBefore) {
        Lesson lesson = courseLessons.get(lessonIndex);
        if (passedLessonIds.contains(lesson.getId())) {
            return null;
        }

        if (Boolean.FALSE.equals(course.getLessonsFreeOrder()) && lessonIndex > 0) {
            Lesson previousLesson = courseLessons.get(lessonIndex - 1);
            if (!passedLessonIds.contains(previousLesson.getId())) {
                return "PREVIOUS_LESSON_NOT_PASSED";
            }
        }

        if (hasUnpassedStopBefore) {
            return "STOP_LESSON_BLOCK";
        }

        return null;
    }
}
