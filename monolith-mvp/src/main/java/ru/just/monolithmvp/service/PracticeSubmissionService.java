package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeSubmissionService {
    private final CourseLessonAdminService courseLessonAdminService;
    private final LessonSubmissionRepository submissionRepository;
    private final AppUserRepository userRepository;
    private final LessonAccessPolicy lessonAccessPolicy;
    private final EnrollmentProgressService enrollmentProgressService;
    private final PracticeScoringPolicy practiceScoringPolicy;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public SubmissionResultDto completeTheoryLesson(Long lessonId, Long userId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        courseAccessPolicy.assertStudentEnrolled(userId, lesson.getCourse().getId());
        lessonAccessPolicy.assertLessonAccessAllowed(userId, lesson);
        lessonAccessPolicy.assertStopLessonAccessAllowed(userId, lesson);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());

        if (!lesson.getLessonType().isTheory()) {
            throw new BadRequestException("Lesson is not THEORY");
        }

        LessonSubmission submission = completeTheoryLessonInternal(userId, lesson);
        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getCompleted(),
                "Theory lesson completed"
        );
    }

    @Transactional
    public SubmissionResultDto submitPractice(Long lessonId, PracticeSubmissionRequest request, Long studentId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        courseAccessPolicy.assertStudentEnrolled(studentId, lesson.getCourse().getId());
        lessonAccessPolicy.assertLessonAccessAllowed(studentId, lesson);
        lessonAccessPolicy.assertStopLessonAccessAllowed(studentId, lesson);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(studentId, lesson.getCourse().getId());

        if (!(lesson instanceof PracticeLesson practiceLesson)
                || (lesson.getLessonType() != LessonType.PRACTICE_TEST
                && lesson.getLessonType() != LessonType.PRACTICE_OPEN_ANSWER)) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        LessonSubmission existingSubmission = submissionRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(null);
        if (existingSubmission != null && Boolean.TRUE.equals(existingSubmission.getCompleted())) {
            throw new BadRequestException("Submission is already finalized");
        }

        validatePracticeAttemptLimit(existingSubmission, practiceLesson.getAttemptLimit());
        validatePracticeTimeLimit(existingSubmission, practiceLesson.getTimeLimitMinutes());

        enrollmentProgressService.markEnrollmentStarted(studentId, lesson.getCourse().getId());
        Map<Integer, List<String>> answersByQuestion = validateAndNormalizeAnswersByQuestion(practiceLesson, request);

        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            return submitOpenPractice(studentId, lesson, practiceLesson, existingSubmission, answersByQuestion);
        }

        return submitTestPractice(studentId, lesson, practiceLesson, existingSubmission, answersByQuestion);
    }

    private SubmissionResultDto submitOpenPractice(Long studentId,
                                                   Lesson lesson,
                                                   PracticeLesson practiceLesson,
                                                   LessonSubmission existingSubmission,
                                                   Map<Integer, List<String>> answersByQuestion) {
        boolean isReworkSubmission = existingSubmission != null && existingSubmission.getStatus() == SubmissionStatus.REWORK;
        if (existingSubmission != null && !isReworkSubmission) {
            throw new BadRequestException("Open submission can be updated only from REWORK status");
        }

        LessonSubmission submission;
        if (existingSubmission != null) {
            submission = existingSubmission;
        } else {
            submission = new LessonSubmission();
            submission.setLesson(lesson);
            AppUser student = new AppUser();
            student.setId(studentId);
            submission.setStudent(student);
        }

        submission.setQuestionProgress(buildOpenQuestionProgressForSubmit(
                practiceLesson,
                answersByQuestion,
                submission.getQuestionProgress(),
                isReworkSubmission
        ));
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setCompleted(false);
        submission.setPointsAwarded(0);
        submission.setAttemptCounter(Optional.ofNullable(submission.getAttemptCounter()).orElse(0) + 1);
        submission.setReviewedByAdminId(null);
        submission.setReviewedAt(null);

        if (submission.getFirstSubmittedAt() == null) {
            submission.setFirstSubmittedAt(LocalDateTime.now());
        }

        submission = submissionRepository.save(submission);
        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                false,
                "Answer submitted and waiting for admin review"
        );
    }

    private SubmissionResultDto submitTestPractice(Long studentId,
                                                   Lesson lesson,
                                                   PracticeLesson practiceLesson,
                                                   LessonSubmission existingSubmission,
                                                   Map<Integer, List<String>> answersByQuestion) {
        LessonSubmission submission;
        if (existingSubmission != null) {
            submission = existingSubmission;
        } else {
            submission = new LessonSubmission();
            submission.setLesson(lesson);
            submission.setStudent(getStudent(studentId));
            submission.setFirstSubmittedAt(LocalDateTime.now());
        }

        int totalQuestionPoints = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                continue;
            }

            List<String> selectedAnswers = answersByQuestion.get(question.getQuestionIndex());
            List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
            totalQuestionPoints += practiceScoringPolicy.scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        // Если набранное кол-во баллов больше порога И лимит попыток не исчерпан - то урок пройден (status=COMPLETED) и completed=true
        // Если набранное кол-во баллов больше порога, но лимит попыток исчерпан (больше нельзя слать результаты) - то status = INCOMPLETE и completed=true
        // Если набранное кол-во баллов меньше порога - status INCOMPLETE, completed=false

        // обновляем номер текущей попытки
        submission.setAttemptCounter(Optional.ofNullable(submission.getAttemptCounter()).orElse(0) + 1);
        boolean isLastAttempt = submission.getAttemptCounter() >= practiceLesson.getAttemptLimit();
        Integer maxPointsByAllQuestions = practiceLesson.getQuestions().stream().map(PracticeQuestion::getFullPoints).reduce(Integer::sum).get();
        boolean passedByPoints = totalQuestionPoints * 100 >= maxPointsByAllQuestions * practiceLesson.getPassingThresholdPercent();
        int lessonPointsAwarded = totalQuestionPoints;

        submission.setQuestionProgress(buildTestQuestionProgress(practiceLesson, answersByQuestion));
        submission.setStatus(passedByPoints ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setCompleted(passedByPoints || isLastAttempt); // Если это последняя попытка - то урок считается завершенным
        submission.setPointsAwarded(lessonPointsAwarded);
        submission = submissionRepository.save(submission);
        if (submission.getCompleted()) {
            enrollmentProgressService.markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        }

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getCompleted(),
                submission.getCompleted() ? "Practice completed" : "Practice is not completed"
        );
    }

    private void validatePracticeAttemptLimit(LessonSubmission existingSubmission, Integer attemptLimit) {
        if (attemptLimit == null || attemptLimit <= 0) {
            return;
        }

        long attempts = existingSubmission == null ? 0 : Optional.ofNullable(existingSubmission.getAttemptCounter()).orElse(0);
        if (attempts >= attemptLimit) {
            throw new BadRequestException("Attempt limit exceeded for this lesson");
        }
    }

    private void validatePracticeTimeLimit(LessonSubmission existingSubmission, Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0 || existingSubmission == null) {
            return;
        }

        LocalDateTime firstSubmittedAt = existingSubmission.getFirstSubmittedAt();
        if (firstSubmittedAt == null) {
            return;
        }

        LocalDateTime deadlineAt = firstSubmittedAt.plusMinutes(timeLimitMinutes);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BadRequestException("Time limit exceeded for this lesson");
        }
    }

    private LessonSubmission completeTheoryLessonInternal(Long studentId, Lesson lesson) {
        Optional<LessonSubmission> existingPassedTheorySubmission = submissionRepository
                .findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(studentId, lesson.getId());
        if (existingPassedTheorySubmission.isPresent()) {
            return existingPassedTheorySubmission.get();
        }

        LessonSubmission submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setStatus(SubmissionStatus.COMPLETE);
        submission.setCompleted(true);
        submission.setPointsAwarded(lesson.getFullPoints());
        submission.setFirstSubmittedAt(LocalDateTime.now());

        enrollmentProgressService.markEnrollmentStarted(studentId, lesson.getCourse().getId());
        submission = submissionRepository.save(submission);
        enrollmentProgressService.markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        return submission;
    }

    private AppUser getStudent(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only STUDENT or ADMIN can submit lessons");
        }
        return user;
    }

    private Map<Integer, List<String>> validateAndNormalizeAnswersByQuestion(PracticeLesson lesson,
                                                                             PracticeSubmissionRequest request) {
        Map<Integer, List<String>> incoming = request.questionAnswers();
        if (incoming == null || incoming.isEmpty()) {
            throw new BadRequestException("questionAnswers is required");
        }

        Set<Integer> lessonQuestionIndexes = lesson.getQuestions().stream()
                .map(PracticeQuestion::getQuestionIndex)
                .collect(Collectors.toSet());

        Set<Integer> incomingQuestionIndexes = incoming.keySet();
        if (!lessonQuestionIndexes.equals(incomingQuestionIndexes)) {
            throw new BadRequestException("questionAnswers must contain answers for all lesson questions and only for them");
        }

        Map<Integer, PracticeQuestion> questionsByIndex = lesson.getQuestions().stream()
                .collect(Collectors.toMap(PracticeQuestion::getQuestionIndex, q -> q));

        Map<Integer, List<String>> normalized = new HashMap<>();
        for (Integer questionIndex : lessonQuestionIndexes) {
            PracticeQuestion question = questionsByIndex.get(questionIndex);
            List<String> answers = normalizeList(incoming.get(questionIndex));

            if (question.getQuestionType() == QuestionType.OPEN_ANSWER || question.getQuestionType() == QuestionType.SINGLE_CHOICE) {
                normalized.put(questionIndex, List.of(answers.getFirst()));
                continue;
            }

            normalized.put(questionIndex, answers);
        }

        return normalized;
    }

    private List<QuestionProgress> buildOpenQuestionProgressForSubmit(PracticeLesson lesson,
                                                                      Map<Integer, List<String>> answersByQuestion,
                                                                      List<QuestionProgress> existingProgress,
                                                                      boolean reworkSubmission) {
        Map<Integer, QuestionProgress> existingProgressByQuestionIndex = Optional.ofNullable(existingProgress)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));

        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .map(question -> {
                    Integer questionIndex = question.getQuestionIndex();
                    QuestionProgress progress = existingProgressByQuestionIndex.getOrDefault(questionIndex, new QuestionProgress());
                    progress.setQuestionIndex(questionIndex);
                    progress.setAnswers(answersByQuestion.getOrDefault(questionIndex, List.of()));
                    progress.setReviewStatus(OpenReviewStatus.PENDING_REVIEW);
                    progress.setAwardedPoints(0);
                    progress.setPointsType(null);

                    return progress;
                }).toList();
    }

    private List<QuestionProgress> buildTestQuestionProgress(PracticeLesson lesson,
                                                             Map<Integer, List<String>> answersByQuestion) {
        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .filter(question -> QuestionType.TEST_QUESTIONS.contains(question.getQuestionType()))
                .map(question -> {
                    List<String> selectedAnswers = answersByQuestion.getOrDefault(question.getQuestionIndex(), List.of());
                    List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
                    QuestionPointsType pointsType = practiceScoringPolicy.resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers);
                    int awardedPoints = switch (pointsType) {
                        case FULL -> question.getFullPoints();
                        case PARTIAL -> question.getPartialPoints();
                        case ZERO -> 0;
                    };

                    return QuestionProgress.builder()
                            .questionIndex(question.getQuestionIndex())
                            .answers(selectedAnswers)
                            .pointsType(pointsType)
                            .awardedPoints(awardedPoints)
                            .reviewStatus(null)
                            .reviewComment(null)
                            .build();
                }).toList();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
