package ru.just.courses.dto.lesson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.lesson.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateLessonDto.HtmlLessonDto.class, name = "HTML"),
        @JsonSubTypes.Type(value = CreateLessonDto.TestLessonDto.class, name = "TEST"),
        @JsonSubTypes.Type(value = CreateLessonDto.MultiTestLessonDto.class, name = "MULTI_TEST"),
        @JsonSubTypes.Type(value = CreateLessonDto.CodeLessonDto.class, name = "CODE"),
        @JsonSubTypes.Type(value = CreateLessonDto.VideoLessonDto.class, name = "VIDEO")
})
public class CreateLessonDto {
    private LessonType type;
    private Integer ordinalNumber;
    private Long themeId;

    public <T extends Lesson> T getModel() {
        throw new UnsupportedOperationException();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HtmlLessonDto extends CreateLessonDto {
        private String html;

        @Override
        public HtmlLesson getModel() {
            final HtmlLesson htmlLesson = new HtmlLesson(getOrdinalNumber(), html);
            htmlLesson.setTheme(new Theme().withId(getThemeId()));
            return htmlLesson;
        }
    } // контент html загружается по другому ендпоинту потоковой передачей

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CodeLessonDto extends CreateLessonDto {
        private String condition;
        private List<CodeLesson.CodeTest> codeTests;

        @Override
        public CodeLesson getModel() {
            final CodeLesson testLesson = new CodeLesson(getOrdinalNumber(), condition, codeTests);
            testLesson.setTheme(new Theme().withId(getThemeId()));
            return testLesson;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestLessonDto extends CreateLessonDto {
        private String condition;
        private List<String> possibleAnswers;
        private String answer;

        @Override
        public TestLesson getModel() {
            final TestLesson testLesson = new TestLesson(getOrdinalNumber(), condition, possibleAnswers, answer);
            testLesson.setTheme(new Theme().withId(getThemeId()));
            return testLesson;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MultiTestLessonDto extends CreateLessonDto {
        private String condition;
        private List<String> possibleAnswers;
        private List<String> correctAnswers;

        @Override
        public MultiTestLesson getModel() {
            final MultiTestLesson testLesson = new MultiTestLesson(getOrdinalNumber(), possibleAnswers, correctAnswers);
            testLesson.setTheme(new Theme().withId(getThemeId()));
            return testLesson;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VideoLessonDto extends CreateLessonDto {
        @Override
        public VideoLesson getModel() {
            final VideoLesson testLesson = new VideoLesson(getOrdinalNumber());
            testLesson.setTheme(new Theme().withId(getThemeId()));
            return testLesson;
        }
    }
}
