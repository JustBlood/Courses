package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.mapper.LessonMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.PracticeQuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CourseLessonAdminService {
    private final LessonRepository lessonRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final LessonMapper lessonMapper;
    private final FileStorageService fileStorageService;
    private final CourseLearnerReadService courseLearnerReadService;

    @Transactional
    public LessonDto createTheoryLesson(Long courseId, CreateTheoryLessonRequest request) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        final int position = resolveCreateLessonPosition(courseId, request.position());
        validateCreateTheoryRequest(request);

        TheoryLesson lesson = new TheoryLesson();
        lesson.setCourse(course);
        lesson.setPosition(position);
        applyTheoryLessonFields(lesson, request);

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto createPracticeLesson(Long courseId, CreatePracticeLessonRequest request) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        validateCreatePracticeRequest(request);
        final Integer lessonPosition = resolveCreateLessonPosition(courseId, request.position());

        PracticeLesson lesson = new PracticeLesson();
        lesson.setCourse(course);
        lesson.setPosition(lessonPosition);
        applyPracticeLessonFields(lesson, request);

        applyQuestionPool(lesson, request.questions());

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public LessonDto getLesson(Long courseId, Long lessonId) {
        final Lesson lesson = getLessonEntity(lessonId);
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }
        return lessonMapper.toDto(lesson);
    }

    @Transactional
    public LessonDto updateTheoryLesson(Long courseId, Long lessonId, UpdateTheoryLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof TheoryLesson lesson)) {
            throw new BadRequestException("Lesson is not theory");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        applyLessonPositionPatch(lesson, request.position());

        applyTheoryLessonFields(lesson, toCreateTheoryLessonRequest(request));

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto updatePracticeLesson(Long courseId, Long lessonId, UpdatePracticeLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof PracticeLesson lesson)) {
            throw new BadRequestException("Lesson is not practice");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        applyLessonPositionPatch(lesson, request.position());

        CreatePracticeLessonRequest patchRequest = toCreatePracticeLessonRequest(request);
        validateUpdatePracticeRequest(patchRequest);
        applyPracticeLessonFields(lesson, patchRequest);

        if (!CollectionUtils.isEmpty(patchRequest.questions())) {
            applyQuestionPool(lesson, patchRequest.questions());
        }

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long courseId, Long lessonId) {
        courseLearnerReadService.getCourseEntity(courseId);
        Lesson lesson = getLessonEntity(lessonId);
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }
        final List<Lesson> lessonsToUpdatePosition = lessonRepository.findByCourse_IdAndPositionGreaterThan(courseId, lesson.getPosition());
        submissionRepository.deleteByLessonId(lessonId);
        practiceQuestionRepository.deleteAllByLessonId(lesson.getId());
        lessonRepository.delete(lesson);
        lessonRepository.flush();
        lessonsToUpdatePosition.forEach(nextLesson -> nextLesson.setPosition(nextLesson.getPosition() - 1));
        lessonRepository.saveAll(lessonsToUpdatePosition);
    }

    @Transactional(readOnly = true)
    public List<LessonDto> getCourseLessons(Long courseId) {
        courseLearnerReadService.getCourseEntity(courseId);
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Lesson getLessonEntity(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ru.just.monolithmvp.exception.NotFoundException("Lesson not found: " + lessonId));
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestion> getPracticeQuestionForLesson(Long lessonId) {
        return practiceQuestionRepository.findByLessonIdOrderByQuestionIndexAsc(lessonId);
    }

    private void applyCommonLessonFields(Lesson lesson,
                                         String title,
                                         String description,
                                         Boolean stopLesson,
                                         Integer attemptLimit,
                                         Integer timeLimitMinutes) {
        lesson.setTitle(Optional.ofNullable(title).orElse(lesson.getTitle()));
        lesson.setDescription(Optional.ofNullable(description).orElse(lesson.getDescription()));
        lesson.setStopLesson(Optional.ofNullable(stopLesson).orElse(lesson.getStopLesson()));
        lesson.setAttemptLimit(Optional.ofNullable(attemptLimit).orElse(lesson.getAttemptLimit()));
        lesson.setTimeLimitMinutes(Optional.ofNullable(timeLimitMinutes).orElse(lesson.getTimeLimitMinutes()));
    }

    private void applyTheoryLessonFields(TheoryLesson lesson, CreateTheoryLessonRequest request) {
        applyCommonLessonFields(lesson, request.title(), request.description(), request.stopLesson(),
                request.attemptLimit(), request.timeLimitMinutes());
        LessonType nextLessonType = patchValue(request.lessonType(), lesson.getLessonType());
        boolean wasPdfLesson = LessonType.THEORY_PDF.equals(lesson.getLessonType());
        if (LessonType.THEORY_PDF.equals(nextLessonType)) {
            if (request.content() != null && !fileStorageService.isFileExistsByRelativePath(request.content())) {
                throw new BadRequestException("file is not exists");
            }
        }
        if (wasPdfLesson && request.content() != null && !Objects.equals(request.content(), lesson.getContent())) {
            fileStorageService.deleteIfExists(lesson.getContent());
        }
        lesson.setLessonType(nextLessonType);
        lesson.setContent(patchValue(request.content(), lesson.getContent()));
        lesson.setFullPoints(patchValue(request.fullPoints(), lesson.getFullPoints()));
    }

    private void applyPracticeLessonFields(PracticeLesson lesson, CreatePracticeLessonRequest request) {
        applyCommonLessonFields(lesson, request.title(), request.description(), request.stopLesson(),
                request.attemptLimit(), request.timeLimitMinutes());
        lesson.setPassingThresholdPercent(patchValue(request.passingThresholdPercent(), lesson.getPassingThresholdPercent()));
        lesson.setShuffleOnEveryAttempt(patchValue(request.shuffleOptions(), lesson.getShuffleOnEveryAttempt()));
        lesson.setShowQuestionStatus(patchValue(request.showQuestionStatus(), lesson.getShowQuestionStatus()));
        lesson.setShowCorrectAnswersAfterCompletion(patchValue(request.showCorrectAnswers(), lesson.getShowCorrectAnswersAfterCompletion()));
        lesson.setLessonType(patchValue(request.lessonType(), lesson.getLessonType()));

        if (request.lessonType().isPractice()) {
            final Integer fullTestLessonPoints = request.questions().stream()
                    .map(PracticeQuestionRequest::fullPoints)
                    .reduce(Integer::sum).orElse(null);
            lesson.setFullPoints(patchValue(fullTestLessonPoints, lesson.getFullPoints()));
        } else {
            lesson.setFullPoints(patchValue(request.fullPoints(), lesson.getFullPoints()));
        }
    }

    private <T> T patchValue(T requestedValue, T currentValue) {
        return requestedValue != null ? requestedValue : currentValue;
    }

    private int nextLessonPosition(Long courseId) {
        Lesson last = lessonRepository.findFirstByCourse_IdOrderByPositionDesc(courseId);
        return last == null ? 1 : last.getPosition() + 1;
    }

    private void applyQuestionPool(PracticeLesson lesson,
                                   List<PracticeQuestionRequest> questions) {
        List<PracticeQuestion> mapped = new ArrayList<>();
        for (PracticeQuestionRequest q : questions) {
            PracticeQuestion entity = new PracticeQuestion();
            entity.setLesson(lesson);
            entity.setQuestionIndex(q.position() == null ? mapped.size() + 1 : q.position());
            entity.setQuestionType(q.questionType());
            entity.setQuestionText(q.questionText());
            entity.setTrainerHint(q.trainerHint());
            entity.setOptions(q.options() == null ? null : new ArrayList<>(q.options()));
            entity.setCorrectAnswers(q.correctAnswers() == null ? null : new ArrayList<>(q.correctAnswers()));
            entity.setFullPoints(q.fullPoints());
            entity.setPartialPoints(q.partialPoints() == null ? 0 : q.partialPoints());
            mapped.add(entity);
        }
        lesson.getQuestions().clear();
        lesson.getQuestions().addAll(mapped);
    }

    private CreateTheoryLessonRequest toCreateTheoryLessonRequest(UpdateTheoryLessonRequest request) {
        return new CreateTheoryLessonRequest(
                request.position(),
                request.title(),
                request.description(),
                null,
                null,
                null,
                request.stopLesson(),
                request.blockedDuringAttempt(),
                request.attemptLimit(),
                request.timeLimitMinutes(),
                request.lessonType(),
                request.content(),
                request.fullPoints(),
                null
        );
    }

    private CreatePracticeLessonRequest toCreatePracticeLessonRequest(UpdatePracticeLessonRequest request) {
        return new CreatePracticeLessonRequest(
                request.position(),
                request.title(),
                request.description(),
                request.stopLesson(),
                request.attemptLimit(),
                request.timeLimitMinutes(),
                request.lessonType(),
                request.fullPoints(),
                request.passingThresholdPercent(),
                request.shuffleOptions(),
                request.showQuestionStatus(),
                request.showCorrectAnswers(),
                request.questions()
        );
    }

    private int resolveCreateLessonPosition(Long courseId, Integer requestedPosition) {
        int nextPosition = nextLessonPosition(courseId);
        if (requestedPosition == null) {
            return nextPosition;
        }

        long lessonCount = lessonRepository.countByCourseId(courseId);
        if (requestedPosition < 1 || requestedPosition > lessonCount + 1) {
            throw new BadRequestException("lesson position must be between 1 and " + (lessonCount + 1));
        }

        if (requestedPosition <= lessonCount) {
            List<Lesson> lessonsToShift = lessonRepository
                    .findByCourse_IdAndPositionGreaterThanEqualOrderByPositionDesc(courseId, requestedPosition);
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() + 1));
            lessonRepository.saveAllAndFlush(lessonsToShift);
        }

        return requestedPosition;
    }

    private void applyLessonPositionPatch(Lesson lesson, Integer requestedPosition) {
        if (requestedPosition == null || requestedPosition.equals(lesson.getPosition())) {
            return;
        }

        long lessonCount = lessonRepository.countByCourseId(lesson.getCourse().getId());
        if (requestedPosition < 1 || requestedPosition > lessonCount) {
            throw new BadRequestException("lesson position must be between 1 and " + lessonCount);
        }

        Integer currentPosition = lesson.getPosition();
        lesson.setPosition(-1);
        lessonRepository.saveAndFlush(lesson);

        List<Lesson> lessonsToShift;
        if (requestedPosition < currentPosition) {
            lessonsToShift = lessonRepository.findByCourse_IdAndPositionBetweenOrderByPositionAsc(
                    lesson.getCourse().getId(),
                    requestedPosition,
                    currentPosition - 1
            );
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() + 1));
        } else {
            lessonsToShift = lessonRepository.findByCourse_IdAndPositionBetweenOrderByPositionAsc(
                    lesson.getCourse().getId(),
                    currentPosition + 1,
                    requestedPosition
            );
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() - 1));
        }

        lessonRepository.saveAll(lessonsToShift);
        lesson.setPosition(requestedPosition);
    }

    private void validateUpdatePracticeRequest(CreatePracticeLessonRequest request) {
        if (request.lessonType() != null && request.lessonType().getSubType() != LessonType.LessonSubType.PRACTICE) {
            throw new BadRequestException("lessonType must be one of practice types");
        }

        if (request.questions() == null) {
            return;
        }

        checkPracticeLessonQuestions(request);
    }

    private void validateCreateTheoryRequest(CreateTheoryLessonRequest request) {
        if (StringUtils.isBlank(request.title())) {
            throw new BadRequestException("title is required");
        }
        if (request.lessonType() == null || request.lessonType().getSubType() != LessonType.LessonSubType.THEORY) {
            throw new BadRequestException("lessonType must be one of theory types");
        }
        if (StringUtils.isBlank(request.content())) {
            throw new BadRequestException("content is required");
        }
    }

    private void validateCreatePracticeRequest(CreatePracticeLessonRequest request) {
        if (StringUtils.isBlank(request.title())) {
            throw new BadRequestException("title is required");
        }
        if (request.lessonType() == null || request.lessonType().getSubType() != LessonType.LessonSubType.PRACTICE) {
            throw new BadRequestException("lessonType must be one of practice types");
        }

        checkPracticeLessonQuestions(request);
    }

    private void checkPracticeLessonQuestions(CreatePracticeLessonRequest request) {
        if (CollectionUtils.isEmpty(request.questions())) {
            throw new BadRequestException("lesson should have 1 question");
        }

        boolean allWithoutPosition = request.questions().stream().allMatch(q -> q.position() == null);
        boolean allWithPosition = request.questions().stream().allMatch(q -> q.position() != null);
        if (!allWithoutPosition && !allWithPosition) {
            throw new BadRequestException("Question positions must be defined for all questions or for no one");
        }

        if (allWithPosition && !request.questions().stream().map(PracticeQuestionRequest::position).collect(Collectors.toSet())
                .equals(IntStream.range(1, request.questions().size() + 1).boxed().collect(Collectors.toSet()))) {
            throw new BadRequestException("Questions positions must be without skipping numbers");
        }

        for (PracticeQuestionRequest q : request.questions()) {
            if (q.partialPoints() != null && q.fullPoints() != null && q.partialPoints() > q.fullPoints()) {
                throw new BadRequestException("partialPoints must be less or equals than fullPoints");
            }
            if (q.questionType() == null) {
                throw new BadRequestException("questionType is required for each question");
            }
            if (StringUtils.isEmpty(q.questionText())) {
                throw new BadRequestException("questionText is required for each question");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && StringUtils.isNoneBlank(q.trainerHint())) {
                throw new BadRequestException("Tests mustn't contain trainerHint");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && CollectionUtils.isEmpty(q.options())) {
                throw new BadRequestException("options are required for test question");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && CollectionUtils.isEmpty(q.correctAnswers())) {
                throw new BadRequestException("correctAnswers are required for test question");
            }
            if (q.questionType() == QuestionType.SINGLE_CHOICE && q.correctAnswers().size() != 1) {
                throw new BadRequestException("SINGLE_CHOICE must contain exactly one correct answer");
            }
            if (q.questionType() == QuestionType.SINGLE_CHOICE && q.partialPoints() != null) {
                throw new BadRequestException("SINGLE_CHOICE mustn't contain partialPoints");
            }
            if (q.questionType() == QuestionType.OPEN_ANSWER && !CollectionUtils.isEmpty(q.correctAnswers())) {
                throw new BadRequestException("OPEN_ANSWER mustn't contain correctAnswers");
            }
            if (q.position() != null && q.position() < 1) {
                throw new BadRequestException("question position must be >= 1");
            }
        }

        boolean hasOpenQuestions = request.questions().stream().anyMatch(q -> q.questionType() == QuestionType.OPEN_ANSWER);
        boolean hasTestQuestions = request.questions().stream().anyMatch(q -> QuestionType.TEST_QUESTIONS.contains(q.questionType()));
        if (hasOpenQuestions && hasTestQuestions) {
            throw new BadRequestException("Practice lesson must contain either only OPEN_ANSWER or only test questions");
        }

        if (request.lessonType() == LessonType.PRACTICE_OPEN_ANSWER && hasTestQuestions) {
            throw new BadRequestException("PRACTICE_OPEN_ANSWER lesson must contain only OPEN_ANSWER questions");
        }

        if (request.lessonType() == LessonType.PRACTICE_TEST && hasOpenQuestions) {
            throw new BadRequestException("PRACTICE_TEST lesson must contain only test questions");
        }
    }
}
