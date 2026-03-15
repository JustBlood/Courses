package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.lesson.LearnerPracticeQuestionDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.model.PracticeQuestion;
import ru.just.monolithmvp.model.QuestionProgress;
import ru.just.monolithmvp.model.TheoryLesson;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseLessonAdminService courseLessonAdminService;
    private final CourseLearnerReadService courseLearnerReadService;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonAccessPolicy lessonAccessPolicy;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public LearnerLessonDto getLessonForLearner(Long lessonId, Long userId) {
        Lesson lesson = courseLessonAdminService.getLessonEntity(lessonId);
        courseAccessPolicy.assertStudentEnrolled(userId, lesson.getCourse().getId());

        boolean lessonAlreadyPassed = isLessonPassedByStudent(userId, lessonId);
        if (!lessonAlreadyPassed) {
            lessonAccessPolicy.assertLessonAccessAllowed(userId, lesson);
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

            Map<Integer, QuestionProgress> questionProgressByIndex = Optional.ofNullable(submission)
                    .map(LessonSubmission::getQuestionProgress)
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));

            boolean showQuestionStatus = Boolean.TRUE.equals(practiceLesson.getShowQuestionStatus());
            boolean showCorrectAnswers = Boolean.TRUE.equals(practiceLesson.getShowCorrectAnswersAfterCompletion())
                    && submission != null
                    && Boolean.TRUE.equals(submission.getCompleted());

            List<PracticeQuestion> practiceQuestions = selectPracticeQuestionsForAttempt(practiceLesson);
            List<LearnerPracticeQuestionDto> questions = practiceQuestions.stream().map(question -> {
                QuestionProgress questionProgress = questionProgressByIndex.get(question.getQuestionIndex());
                return new LearnerPracticeQuestionDto(
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
                                ? Optional.ofNullable(questionProgress).map(QuestionProgress::getAwardedPoints).orElse(null)
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

    @Transactional(readOnly = true)
    public LearnerLessonDto getNextLessonForLearner(Long courseId, Long userId) {
        Long nextLessonId = courseLearnerReadService.findNextLessonIdForLearner(userId, courseId);
        if (nextLessonId == null) {
            throw new BadRequestException("No next lesson available");
        }
        return getLessonForLearner(nextLessonId, userId);
    }

    private boolean isLessonPassedByStudent(Long studentId, Long lessonId) {
        return submissionRepository
                .findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(studentId, lessonId)
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
