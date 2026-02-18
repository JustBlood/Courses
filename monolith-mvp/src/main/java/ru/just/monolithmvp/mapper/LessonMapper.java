package ru.just.monolithmvp.mapper;

import org.springframework.stereotype.Component;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.lesson.PracticeQuestionDto;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.model.PracticeQuestion;
import ru.just.monolithmvp.model.TheoryLesson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                lesson.getCoverFilePath(),
                lesson.isRequiresPreviousCompleted(),
                lesson.isOpenForAccess(),
                lesson.getStopLesson(),
                lesson.getBlockedDuringAttempt(),
                lesson.getAttemptLimit(),
                lesson.getTimeLimitMinutes(),
                lesson.getLessonType(),
                theoryLesson == null ? null : theoryLesson.getContentType(),
                theoryLesson == null ? null : theoryLesson.getContent(),
                lesson.getFullPoints(),
                lesson.getPartialPoints(),
                practiceLesson == null ? null : practiceLesson.getPassingThresholdPercent(),
                practiceLesson == null ? null : practiceLesson.getEvaluateByCorrectCount(),
                practiceLesson == null ? null : practiceLesson.getRandomQuestionCount(),
                practiceLesson == null ? null : practiceLesson.getShuffleOptions(),
                practiceLesson == null ? null : practiceLesson.getShowQuestionStatus(),
                practiceLesson == null ? null : practiceLesson.getShowCorrectAnswers(),
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
                        splitRaw(q.getOptionsRaw()),
                        splitRaw(q.getCorrectAnswersRaw()),
                        q.getFullPoints(),
                        q.getPartialPoints()
                ))
                .toList();
    }

    public List<String> splitRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(";;", -1)).toList();
    }
}
