package ru.just.progressservice.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "taskType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TaskDto.TestTaskDto.class, name = "TEST"),
        @JsonSubTypes.Type(value = TaskDto.MultiTestTaskDto.class, name = "MULTI_TEST"),
        @JsonSubTypes.Type(value = TaskDto.CodeTaskDto.class, name = "CODE"),
        @JsonSubTypes.Type(value = TaskDto.TheoryTaskDto.class, name = "THEORY")
})
public class TaskDto {
    private TaskType taskType;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestTaskDto extends TaskDto {
        private String userAnswer;
        private String correctAnswer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MultiTestTaskDto extends TaskDto {
        private List<String> userAnswers;
        private List<String> correctAnswers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CodeTaskDto extends TaskDto {
        private List<CodeTest> codeTests;
        private String userCode;

        public record CodeTest(Long testId, String input, String output) { }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TheoryTaskDto extends TaskDto { }
}
