package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.lesson.LearnerPracticeQuestionDto;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseLessonAdminService courseLessonAdminService;
    private final CourseLearnerReadService courseLearnerReadService;
    private final PracticeScoringPolicy practiceScoringPolicy;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonAccessPolicy lessonAccessPolicy;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional(readOnly = true)
    public LearnerLessonDto getLessonForLearner(Long lessonId, Long userId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        courseAccessPolicy.assertStudentEnrolled(userId, lesson.getCourse().getId());

        boolean lessonFinalized = isLessonFinalizedByStudent(userId, lessonId);
        if (!lessonFinalized) {
            lessonAccessPolicy.assertLessonAccessAllowed(userId, lessonId);
            lessonAccessPolicy.assertStopLessonAccessAllowed(userId, lesson);
            courseAccessPolicy.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());
        }

        LearnerLessonDto.LearnerLessonDtoBuilder learnerLessonDtoBuilder = LearnerLessonDto.builder()
                .id(lesson.getId())
                .position(lesson.getPosition())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonType(lesson.getLessonType());

        if (LessonType.LessonSubType.PRACTICE.equals(lesson.getLessonType().getSubType())) {
            PracticeLesson practiceLesson = (PracticeLesson) lesson;
            LessonSubmission submission = submissionRepository.findByStudentIdAndLessonId(userId, lessonId)
                    .orElse(null);

            Map<Long, QuestionProgress> questionProgressById = Optional.ofNullable(submission)
                    .map(LessonSubmission::getQuestionProgress)
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(QuestionProgress::getQuestionId, q -> q, (left, right) -> right));

            boolean showQuestionStatus = Boolean.TRUE.equals(practiceLesson.getShowQuestionStatus());
            boolean showCorrectAnswers = Boolean.TRUE.equals(practiceLesson.getShowCorrectAnswersAfterCompletion())
                    && submission != null
                    && SubmissionStatus.COMPLETED == submission.getStatus();

            List<PracticeQuestion> practiceQuestions = selectPracticeQuestionsForAttempt(practiceLesson);
            List<LearnerPracticeQuestionDto> questions = practiceQuestions.stream().map(question -> {
                QuestionProgress questionProgress = questionProgressById.get(question.getId());
                return new LearnerPracticeQuestionDto(
                        question.getId(),
                        question.getQuestionIndex(),
                        question.getQuestionType(),
                        question.getQuestionText(),
                        Optional.ofNullable(question.getOptions()).orElse(List.of()),
                        questionProgress != null ? questionProgress.getAnswers() : List.of(),
                        showCorrectAnswers && question.getCorrectAnswers() != null && !question.getCorrectAnswers().isEmpty()
                                ? question.getCorrectAnswers()
                                : null,
                        showQuestionStatus
                                ? Optional.ofNullable(questionProgress).map(QuestionProgress::getReviewStatus).orElse(null)
                                : null,
                        questionProgress != null ? questionProgress.getReviewComment() : null,
                        showQuestionStatus
                                ? Optional.ofNullable(questionProgress)
                                    .filter(progress -> progress.getPointsType() != null)
                                    .map(progress -> practiceScoringPolicy.scoreQuestion(progress.getPointsType(), question)).orElse(null)
                                : null,
                        question.getFullPoints(),
                        question.getPartialPoints()
                );
            }).toList();

            learnerLessonDtoBuilder.questions(questions);
        } else {
            TheoryLesson theoryLesson = (TheoryLesson) lesson;
            learnerLessonDtoBuilder.theoryContent(theoryLesson.getContent());
        }

        return learnerLessonDtoBuilder.build();
    }

    @Transactional
    public LearnerLessonDto getNextLessonForLearner(Long courseId, Long userId) {
        Long nextLessonId = courseLearnerReadService.findNextLessonIdForLearner(userId, courseId);
        if (nextLessonId == null) {
            throw new NotFoundException("No next lesson available");
        }
        return getLessonForLearner(nextLessonId, userId);
    }

    private boolean isLessonFinalizedByStudent(Long studentId, Long lessonId) {
        return submissionRepository
                .findFirstByStudentIdAndLessonIdAndStatusInOrderBySubmittedAtDesc(studentId, lessonId, SubmissionStatus.FINAL_STATUSES)
                .isPresent();
    }

    private List<PracticeQuestion> selectPracticeQuestionsForAttempt(PracticeLesson practiceLesson) {
        List<PracticeQuestion> selectedQuestions = new ArrayList<>(courseLessonAdminService.getPracticeQuestionForLesson(practiceLesson.getId()));

        if (Boolean.TRUE.equals(practiceLesson.getShuffleOnEveryAttempt())) {
            Collections.shuffle(selectedQuestions);
        } else {
            selectedQuestions.sort(Comparator.comparing(PracticeQuestion::getQuestionIndex));
        }

        return selectedQuestions;
    }
}
