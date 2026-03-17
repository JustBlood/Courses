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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearnerReadService {
    private final CourseRepository courseRepository;
    private final PracticeScoringPolicy practiceScoringPolicy;
    private final CourseProgressService courseProgressService;
    private final CourseProgressRepository courseProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final FileStorageService fileStorageService;

    @Transactional
    public CourseLearnerDto getCourseForLearner(Long userId, Long courseId) {
        courseAccessPolicy.assertStudentEnrolled(userId, courseId);

        if (courseAccessPolicy.isCourseDeadlineExceeded(userId, courseId)) {
            courseProgressService.recalcCourseProgressByUser(userId, courseId);
        }

        Course course = getCourseEntity(courseId);
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
        Map<Long, LessonSubmission> submissionsByLessonId = loadSubmissionsByLessonId(userId, courseId);
        List<LearnerLessonSummaryDto> lessons = buildLearnerLessonSummaries(course, courseLessons, submissionsByLessonId);

        final Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        final Optional<CourseProgress> courseProgress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        int totalLessons = lessons.size();
        int completedLessons = (int) submissionsByLessonId.values().stream().map(LessonSubmission::getStatus).filter(SubmissionStatus.COMPLETED::equals).count();
        int remainingLessons = Math.max(totalLessons - completedLessons, 0);
        int completionPercent = totalLessons == 0 ? 0 : (completedLessons * 100) / totalLessons;

        CourseProgressDto progress = null;
        if (enrollment != null) {
            progress = new CourseProgressDto(
                    enrollment.getEnrolledAt().plusDays(course.getDeadlineDays()),
                    completionPercent,
                    completedLessons,
                    remainingLessons,
                    courseProgress.map(CourseProgress::getStatus).orElse(null)
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

    @Transactional
    public List<CourseLearnerDto> getCoursesForLearner(Long userId) {
        final List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

        List<CourseLearnerDto> coursesForLearner = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            coursesForLearner.add(getCourseForLearner(userId, course.getId()));
        }
        return coursesForLearner;
    }

    @Transactional(readOnly = true)
    public Long findNextLessonIdForLearner(Long userId, Long courseId) {
        courseAccessPolicy.assertStudentEnrolled(userId, courseId);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, courseId);

        Course course = getCourseEntity(courseId);
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
        final Map<Long, LessonSubmission> submissionsByLessonId = loadSubmissionsByLessonId(userId, courseId);
        Set<Long> completedLessonIds = resolveCompletedLessonIds(submissionsByLessonId);
        Set<Long> answeredLessonIds = resolveAnsweredLessonIds(submissionsByLessonId);

        boolean hasUnpassedStopBefore = false;
        for (int index = 0; index < courseLessons.size(); index++) {
            Lesson lesson = courseLessons.get(index);
            boolean completed = completedLessonIds.contains(lesson.getId());
            if (completed) {
                continue;
            }

            boolean blockedByPreviousLesson = Boolean.FALSE.equals(course.getLessonsFreeOrder())
                    && index > 0
                    && !answeredLessonIds.contains(courseLessons.get(index - 1).getId());

            if (!blockedByPreviousLesson && !hasUnpassedStopBefore && !answeredLessonIds.contains(lesson.getId())) {
                return lesson.getId();
            }

            if (Boolean.TRUE.equals(lesson.getStopLesson()) && answeredLessonIds.contains(lesson.getId())) {
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

    private Set<Long> resolveCompletedLessonIds(Map<Long, LessonSubmission> submissionsByLessonId) {
        return submissionsByLessonId.entrySet().stream()
                .filter(entry -> SubmissionStatus.COMPLETED == entry.getValue().getStatus())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<Long> resolveAnsweredLessonIds(Map<Long, LessonSubmission> submissionsByLessonId) {
        return submissionsByLessonId.entrySet().stream()
                .filter(entry -> SubmissionStatus.ALLOW_GET_NEXT_LESSON_STATUSES.contains(entry.getValue().getStatus()))
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

            int pointsAwarded;
            int fullPoints;
            if (lesson instanceof PracticeLesson practiceLesson) {
                final Map<Long, PracticeQuestion> questionsById = practiceLesson.getQuestions().stream()
                        .collect(Collectors.toMap(PracticeQuestion::getId, q -> q));
            pointsAwarded = submission != null
                        ? submission.getQuestionProgress().stream()
                            .filter(progress -> progress.getPointsType()  != null)
                            .map(progress -> practiceScoringPolicy.scoreQuestion(progress.getPointsType(), questionsById.get(progress.getQuestionId())))
                            .reduce(Integer::sum).orElse(0)
                        : 0;
            fullPoints = practiceLesson.getMaxPointsByAllQuestions();
            } else {
                pointsAwarded = submission != null ? lesson.getFullPoints() : 0;
                fullPoints = lesson.getFullPoints();
            }


            final SubmissionStatus submissionStatus = Optional.ofNullable(submission)
                    .map(LessonSubmission::getStatus)
                    .orElse(null);

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
                    fullPoints,
                    new LessonProgressDto(
                        submissionStatus,
                        pointsAwarded
                    )
            ));

            allPreviousLessonsPassed = allPreviousLessonsPassed && submissionStatus != null
                    && SubmissionStatus.ALLOW_GET_NEXT_LESSON_STATUSES.contains(submissionStatus);

            hasUnpassedStopBefore = hasUnpassedStopBefore || lesson.getStopLesson() && submissionStatus != null
                    && submissionStatus != SubmissionStatus.COMPLETED;
        }

        return lessons;
    }
}
