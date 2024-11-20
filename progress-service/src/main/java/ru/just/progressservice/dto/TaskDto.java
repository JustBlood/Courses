package ru.just.progressservice.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
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

        public TestTaskDto(String userAnswer, String correctAnswer) {
            super(TaskType.TEST);
            this.userAnswer = userAnswer;
            this.correctAnswer = correctAnswer;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MultiTestTaskDto extends TaskDto {
        private List<String> userAnswers;
        private List<String> correctAnswers;

        public MultiTestTaskDto(List<String> userAnswers, List<String> correctAnswers) {
            super(TaskType.MULTI_TEST);
            this.userAnswers = userAnswers;
            this.correctAnswers = correctAnswers;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CodeTaskDto extends TaskDto {
        private List<CodeTest> codeTests;
        private String userCode;

        public CodeTaskDto(List<CodeTest> codeTests, String userCode) {
            super(TaskType.CODE);
            this.codeTests = codeTests;
            this.userCode = userCode;
        }
    }

    @Getter
    @Setter
    public static class TheoryTaskDto extends TaskDto {
        public TheoryTaskDto() {
            super(TaskType.THEORY);
        }
    }
}
