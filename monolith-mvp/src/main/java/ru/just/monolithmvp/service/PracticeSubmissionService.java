package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeSubmissionService {
    private final CourseLessonAdminService courseLessonAdminService;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonAccessPolicy lessonAccessPolicy;
    private final CourseProgressService courseProgressService;
    private final PracticeScoringPolicy practiceScoringPolicy;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public SubmissionResultDto completeTheoryLesson(Long lessonId, Long userId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        validateLessonAllowedRules(lessonId, userId, lesson);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());

        if (!lesson.getLessonType().isTheory()) {
            throw new BadRequestException("Lesson is not THEORY");
        }

        LessonSubmission submission = completeTheoryLessonInternal(userId, lesson);
        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus()
        );
    }

    @Transactional
    public SubmissionResultDto submitPractice(Long lessonId, PracticeSubmissionRequest request, Long studentId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        validateLessonAllowedRules(lessonId, studentId, lesson);
        courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(studentId, lesson.getCourse().getId());

        if (!(lesson instanceof PracticeLesson practiceLesson)
                || (lesson.getLessonType() != LessonType.PRACTICE_TEST
                && lesson.getLessonType() != LessonType.PRACTICE_OPEN_ANSWER)) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        LessonSubmission existingSubmission = submissionRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElseThrow(() -> new BadRequestException("Lesson not started"));
        if (!(existingSubmission.getStatus() == SubmissionStatus.STARTED)) {
            throw new BadRequestException("Practice submission can be updated only from REWORKING or STARTED status");
        }

        validatePracticeAttemptLimit(existingSubmission, practiceLesson.getAttemptLimit());
        validatePracticeTimeLimit(existingSubmission, practiceLesson.getTimeLimitMinutes(), request.submittedAt());

        Map<Long, List<String>> answersByQuestion = validateAndNormalizeAnswersByQuestion(practiceLesson, request);

        SubmissionResultDto submissionResultDto;
        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            submissionResultDto = submitOpenPractice(practiceLesson, existingSubmission, answersByQuestion);
        } else {
            submissionResultDto = submitTestPractice(practiceLesson, existingSubmission, answersByQuestion);
        }
        courseProgressService.recalcCourseProgressByUser(studentId, lesson.getCourse().getId());

        return submissionResultDto;
    }

    @Transactional
    public SubmissionResultDto startLesson(Long lessonId, Long studentId) {
        final Lesson lessonEntity = courseLessonAdminService.getLessonEntity(lessonId);
        validateLessonAllowedRules(lessonId, studentId, lessonEntity);

        LessonSubmission existingSubmission = submissionRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(null);

        if (existingSubmission != null) {
            if (existingSubmission.getStatus() != SubmissionStatus.REWORKING) {
                throw new BadRequestException("Lesson already started");
            }
            existingSubmission.setStatus(SubmissionStatus.STARTED);
            existingSubmission.setStartedAt(LocalDateTime.now(Clock.systemUTC()));
            submissionRepository.save(existingSubmission);
            return new SubmissionResultDto(existingSubmission.getId(), existingSubmission.getStatus());
        }

        LessonSubmission submission = new LessonSubmission();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        submission.setLesson(lesson);
        AppUser student = new AppUser();
        student.setId(studentId);
        submission.setStudent(student);
        submission.setStartedAt(LocalDateTime.now(Clock.systemUTC()));
        submission.setStatus(SubmissionStatus.STARTED);

        submission = submissionRepository.save(submission);
        courseProgressService.markCourseProgressStarted(studentId, lessonEntity.getCourse().getId(), submission.getStartedAt());

        return new SubmissionResultDto(submission.getId(), submission.getStatus());
    }

    private void validateLessonAllowedRules(Long lessonId, Long studentId, Lesson lesson) {
        courseAccessPolicy.assertStudentEnrolled(studentId, lesson.getCourse().getId());
        lessonAccessPolicy.assertLessonAccessAllowed(studentId, lessonId);
        lessonAccessPolicy.assertStopLessonAccessAllowed(studentId, lesson);
    }

    private SubmissionResultDto submitOpenPractice(PracticeLesson practiceLesson,
                                                   LessonSubmission existingSubmission,
                                                   Map<Long, List<String>> answersByQuestion) {
        existingSubmission.setQuestionProgress(buildOpenQuestionProgressForSubmit(
                practiceLesson,
                answersByQuestion,
                existingSubmission.getQuestionProgress()
        ));
        existingSubmission.setStatus(SubmissionStatus.PENDING_REVIEW);
        existingSubmission.setAttemptCounter(Optional.ofNullable(existingSubmission.getAttemptCounter()).orElse(0) + 1);
        existingSubmission.setReviewedByAdminId(null);
        existingSubmission.setReviewedAt(null);
        existingSubmission.setStartedAt(null);
        existingSubmission.setSubmittedAt(LocalDateTime.now(Clock.systemUTC()));

        existingSubmission = submissionRepository.saveAndFlush(existingSubmission);
        return new SubmissionResultDto(
                existingSubmission.getId(),
                existingSubmission.getStatus()
        );
    }

    private SubmissionResultDto submitTestPractice(PracticeLesson practiceLesson,
                                                   LessonSubmission existingSubmission,
                                                   Map<Long, List<String>> answersByQuestion) {
        existingSubmission.setSubmittedAt(LocalDateTime.now(Clock.systemUTC()));

        int totalAwardedQuestionPoints = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                throw new IllegalStateException("Test lesson contains non-tests questions!");
            }

            List<String> selectedAnswers = answersByQuestion.get(question.getId());
            List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
            totalAwardedQuestionPoints += practiceScoringPolicy.scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        // Если набранное кол-во баллов больше порога И лимит попыток не исчерпан - то урок пройден (status=COMPLETED) и completed=true
        // Если набранное кол-во баллов больше порога, но лимит попыток исчерпан (больше нельзя слать результаты) - то status = INCOMPLETE и completed=true
        // Если набранное кол-во баллов меньше порога - status INCOMPLETE, completed=false

        // обновляем номер текущей попытки
        boolean passedByPoints = practiceLesson.passedByPoints(totalAwardedQuestionPoints);
        existingSubmission.setAttemptCounter(Optional.ofNullable(existingSubmission.getAttemptCounter()).orElse(0) + 1);
        boolean isLastAttempt = practiceLesson.getAttemptLimit() != null && existingSubmission.getAttemptCounter() >= practiceLesson.getAttemptLimit();

        existingSubmission.setQuestionProgress(buildTestQuestionProgress(practiceLesson, answersByQuestion));
        existingSubmission.setStatus(passedByPoints ? SubmissionStatus.COMPLETED : isLastAttempt ? SubmissionStatus.INCOMPLETED : SubmissionStatus.REWORKING);
        existingSubmission.setStartedAt(existingSubmission.getStatus() == SubmissionStatus.REWORKING ? null : existingSubmission.getStartedAt());
        existingSubmission = submissionRepository.saveAndFlush(existingSubmission);

        return new SubmissionResultDto(
                existingSubmission.getId(),
                existingSubmission.getStatus()
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

    private void validatePracticeTimeLimit(LessonSubmission existingSubmission, Integer timeLimitMinutes, Long submittedAtEpochSeconds) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0 || existingSubmission == null) {
            return;
        }

        LocalDateTime startedAt = existingSubmission.getStartedAt();
        if (startedAt == null) {
            return;
        }

        final LocalDateTime submittedAtPlusBuffer = LocalDateTime.ofEpochSecond(submittedAtEpochSeconds, 0, ZoneOffset.UTC)
                .plusSeconds(15);

        LocalDateTime deadlineAt = startedAt.plusMinutes(timeLimitMinutes);
        if (submittedAtPlusBuffer.isAfter(deadlineAt)) {
            throw new BadRequestException("Time limit exceeded for this lesson");
        }
    }

    private LessonSubmission completeTheoryLessonInternal(Long studentId, Lesson lesson) {
        Optional<LessonSubmission> existingSubmission = submissionRepository
                .findByStudentIdAndLessonId(studentId, lesson.getId());
        if (existingSubmission.isEmpty()) {
            throw new BadRequestException("Theory lesson not started.");
        }

        if (SubmissionStatus.COMPLETED == existingSubmission.get().getStatus()) {
            return existingSubmission.get();
        }

        LessonSubmission submission = existingSubmission.get();
        submission.setStatus(SubmissionStatus.COMPLETED);
        submission.setSubmittedAt(LocalDateTime.now(Clock.systemUTC()));

        submission = submissionRepository.saveAndFlush(submission);
        courseProgressService.recalcCourseProgressByUser(studentId, lesson.getCourse().getId());
        return submission;
    }

    private Map<Long, List<String>> validateAndNormalizeAnswersByQuestion(PracticeLesson lesson,
                                                                             PracticeSubmissionRequest request) {
        Set<Long> lessonQuestionIds = lesson.getQuestions().stream()
                .map(PracticeQuestion::getId)
                .collect(Collectors.toSet());

        Map<Long, List<String>> incoming = request.questionAnswers();
        if (incoming == null || incoming.isEmpty()) {
            incoming = new HashMap<>();
        }

        for (Long questionId : lessonQuestionIds) {
            incoming.putIfAbsent(questionId, new ArrayList<>());
        }

        Set<Long> incomingQuestionIndexes = incoming.keySet();
        if (!lessonQuestionIds.equals(incomingQuestionIndexes)) {
            throw new BadRequestException("questionAnswers must contain answers for all lesson questions and only for them");
        }

        Map<Long, PracticeQuestion> questionsById = lesson.getQuestions().stream()
                .collect(Collectors.toMap(PracticeQuestion::getId, q -> q));

        Map<Long, List<String>> normalized = new HashMap<>();
        for (Long questionId : lessonQuestionIds) {
            PracticeQuestion question = questionsById.get(questionId);
            List<String> answers = normalizeList(incoming.get(questionId));

            if (question.getQuestionType() == QuestionType.OPEN_ANSWER || question.getQuestionType() == QuestionType.SINGLE_CHOICE) {
                normalized.put(questionId, answers);
                continue;
            }

            normalized.put(questionId, answers);
        }

        return normalized;
    }

    private List<QuestionProgress> buildOpenQuestionProgressForSubmit(PracticeLesson lesson,
                                                                      Map<Long, List<String>> answersByQuestion,
                                                                      List<QuestionProgress> existingProgress) {
        Map<Long, QuestionProgress> existingProgressByQuestionId = Optional.ofNullable(existingProgress)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionId, q -> q, (left, right) -> right));

        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .map(question -> {
                    QuestionProgress progress = existingProgressByQuestionId.getOrDefault(question.getId(), new QuestionProgress());
                    // вопросы, которые УЖЕ финализированы - не обновляются. Здесь только новые и не-финальные
                    if (progress.getReviewStatus() == null || !progress.getReviewStatus().isFinal()) {
                        progress.setQuestionId(question.getId());
                        progress.setAnswers(answersByQuestion.getOrDefault(question.getId(), List.of()));
                        progress.setReviewStatus(OpenReviewStatus.PENDING_REVIEW);
                        progress.setPointsType(null);
                    }

                    return progress;
                }).toList();
    }

    private List<QuestionProgress> buildTestQuestionProgress(PracticeLesson lesson,
                                                             Map<Long, List<String>> answersByQuestion) {
        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .filter(question -> QuestionType.TEST_QUESTIONS.contains(question.getQuestionType()))
                .map(question -> {
                    List<String> selectedAnswers = answersByQuestion.getOrDefault(question.getId(), List.of());
                    List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
                    QuestionPointsType pointsType = practiceScoringPolicy.resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers);

                    return QuestionProgress.builder()
                            .questionId(question.getId())
                            .answers(selectedAnswers)
                            .pointsType(pointsType)
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
