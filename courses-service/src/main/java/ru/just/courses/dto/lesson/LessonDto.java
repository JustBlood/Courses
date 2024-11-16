package ru.just.courses.dto.lesson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.just.courses.model.theme.lesson.CodeLesson;
import ru.just.courses.model.theme.lesson.LessonType;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LessonDto.HtmlLessonDto.class, name = "HTML"),
        @JsonSubTypes.Type(value = LessonDto.TestLessonDto.class, name = "TEST"),
        @JsonSubTypes.Type(value = LessonDto.MultiTestLessonDto.class, name = "MULTI_TEST"),
        @JsonSubTypes.Type(value = LessonDto.CodeLessonDto.class, name = "CODE"),
        @JsonSubTypes.Type(value = LessonDto.VideoLessonDto.class, name = "VIDEO")
})
public class LessonDto {
    private Long lessonId;
    private LessonType type;
    private Integer ordinalNumber;

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    public static class HtmlLessonDto extends LessonDto { } // контент html получается по другому ендпоинту потоковой передачей

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class CodeLessonDto extends LessonDto {
        private String condition;
        private List<CodeLesson.CodeTest> codeTests;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class TestLessonDto extends LessonDto {
        private String condition;
        private List<String> possibleAnswers;
        private String answer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class MultiTestLessonDto extends LessonDto {
        private String condition;
        private List<String> possibleAnswers;
        private List<String> correctAnswers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class VideoLessonDto extends LessonDto {
        private String videoUrl;
    }
}
