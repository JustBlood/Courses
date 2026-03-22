package ru.just.monolithmvp.mapper;

import org.springframework.stereotype.Component;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.lesson.PracticeQuestionDto;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.model.PracticeQuestion;
import ru.just.monolithmvp.model.TheoryLesson;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class LessonMapper {

    public LessonDto toDto(Lesson lesson) {
        TheoryLesson theoryLesson = lesson instanceof TheoryLesson t ? t : null;
        PracticeLesson practiceLesson = lesson instanceof PracticeLesson p ? p : null;

        return new LessonDto(
                lesson.getId(),
                lesson.getCourse() == null ? null : lesson.getCourse().getId(),
                lesson.getPosition(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getStopLesson(),
                lesson.getAttemptLimit(),
                lesson.getTimeLimitMinutes(),
                lesson.getLessonType(),
                theoryLesson == null ? null : theoryLesson.getContent(),
                lesson.getFullPoints(),
                practiceLesson == null ? null : practiceLesson.getPassingThresholdPercent(),
                practiceLesson == null ? null : practiceLesson.getShuffleOnEveryAttempt(),
                practiceLesson == null ? null : practiceLesson.getShowCorrectAnswersAfterCompletion(),
                practiceLesson == null ? Collections.emptyList() : toQuestionDtos(practiceLesson.getQuestions())
        );
    }

    private List<PracticeQuestionDto> toQuestionDtos(List<PracticeQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }
        return questions.stream()
                .map(q -> new PracticeQuestionDto(
                        q.getId(),
                        q.getQuestionIndex(),
                        q.getQuestionType(),
                        q.getQuestionText(),
                        q.getTrainerHint(),
                        Optional.ofNullable(q.getOptions()).orElse(List.of()),
                        Optional.ofNullable(q.getCorrectAnswers()).orElse(List.of()),
                        q.getFullPoints(),
                        q.getPartialPoints()
                ))
                .toList();
    }
}
