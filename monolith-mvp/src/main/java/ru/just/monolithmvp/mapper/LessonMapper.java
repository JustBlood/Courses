package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.model.TheoryLesson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "courseId", expression = "java(lesson.getCourse().getId())")
    @Mapping(target = "theoryContentType", expression = "java(extractTheoryContentType(lesson))")
    @Mapping(target = "theoryContent", expression = "java(extractTheoryContent(lesson))")
    @Mapping(target = "questionType", expression = "java(extractQuestionType(lesson))")
    @Mapping(target = "questionText", expression = "java(extractQuestionText(lesson))")
    @Mapping(target = "options", expression = "java(extractOptions(lesson))")
    LessonDto toDto(Lesson lesson);

    default ru.just.monolithmvp.model.TheoryContentType extractTheoryContentType(Lesson lesson) {
        if (lesson instanceof TheoryLesson theoryLesson) {
            return theoryLesson.getContentType();
        }
        return null;
    }

    default String extractTheoryContent(Lesson lesson) {
        if (lesson instanceof TheoryLesson theoryLesson) {
            return theoryLesson.getContent();
        }
        return null;
    }

    default ru.just.monolithmvp.model.QuestionType extractQuestionType(Lesson lesson) {
        if (lesson instanceof PracticeLesson practiceLesson) {
            return practiceLesson.getQuestionType();
        }
        return null;
    }

    default String extractQuestionText(Lesson lesson) {
        if (lesson instanceof PracticeLesson practiceLesson) {
            return practiceLesson.getQuestionText();
        }
        return null;
    }

    default List<String> extractOptions(Lesson lesson) {
        if (lesson instanceof PracticeLesson practiceLesson) {
            return splitRaw(practiceLesson.getOptionsRaw());
        }
        return Collections.emptyList();
    }

    default List<String> splitRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(";;", -1)).toList();
    }
}
