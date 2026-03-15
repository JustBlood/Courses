package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.course.CourseLearnerDto;
import ru.just.monolithmvp.dto.course.CourseProgressDto;
import ru.just.monolithmvp.dto.lesson.LearnerLessonSummaryDto;
import ru.just.monolithmvp.dto.lesson.LessonProgressDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearnerReadService {
    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
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
        List<LearnerLessonSummaryDto> lessons = buildLearnerLessonSummaries(course, courseLessons, submissionsByLessonId);

        final Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        final CourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);
        int totalLessons = lessons.size();
        int completedLessons = (int) lessons.stream().filter(summary -> summary.lessonProgress().completed()).count();
        int remainingLessons = Math.max(totalLessons - completedLessons, 0);
        int completionPercent = totalLessons == 0 ? 100 : (completedLessons * 100) / totalLessons;

        CourseProgressDto progress = null;
        if (enrollment != null) {
            progress = new CourseProgressDto(
                    enrollment.getEnrolledAt().plusDays(course.getDeadlineDays()),
                    completionPercent,
                    completedLessons,
                    remainingLessons,
                    courseProgress != null ? courseProgress.getStatus() : null
            );
        }

        return new CourseLearnerDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                fileStorageService.normalizeStoredPath(course.getCoverFilePath()),
                course.getDeadlineDays(),
                totalLessons,
                progress,
                lessons
        );
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
                                                                       Map<Long, LessonSubmission> submissionsByLessonId) {
        List<LearnerLessonSummaryDto> lessons = new ArrayList<>(courseLessons.size());
        boolean hasUnpassedStopBefore = false;
        boolean allPreviousLessonsPassed = true;

        for (int index = 0; index < courseLessons.size(); index++) {
            Lesson lesson = courseLessons.get(index);

            final LessonSubmission submission = submissionsByLessonId.get(lesson.getId());
            Integer pointsAwarded = Optional.ofNullable(submission)
                    .map(LessonSubmission::getPointsAwarded)
                    .orElse(0);
            final SubmissionStatus submissionStatus = Optional.ofNullable(submission)
                    .map(LessonSubmission::getStatus)
                    .orElse(null);
            Boolean completed = Optional.ofNullable(submission)
                    .map(LessonSubmission::getCompleted)
                    .orElse(false);
            allPreviousLessonsPassed = allPreviousLessonsPassed && completed;

            String blockReason = null;
            if (!course.getLessonsFreeOrder() && index > 0 && !allPreviousLessonsPassed) {
                blockReason = "PREVIOUS_LESSONS_NOT_PASSED";
            }
            if (hasUnpassedStopBefore) {
                blockReason = "STOP_LESSON_BLOCK";
            }

            boolean blocked = blockReason != null;

            lessons.add(new LearnerLessonSummaryDto(
                    lesson.getId(),
                    lesson.getPosition(),
                    lesson.getTitle(),
                    lesson.getLessonType(),
                    blocked,
                    blockReason,
                    new LessonProgressDto(
                        completed,
                        submissionStatus,
                        pointsAwarded
                    )
            ));

            hasUnpassedStopBefore = hasUnpassedStopBefore || lesson.getStopLesson() && !completed;
        }

        return lessons;
    }
}
