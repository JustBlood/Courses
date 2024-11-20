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
        @JsonSubTypes.Type(value = UserAnswerDto.TestUserAnswerDto.class, name = "TEST"),
        @JsonSubTypes.Type(value = UserAnswerDto.MultiTestUserAnswerDto.class, name = "MULTI_TEST"),
        @JsonSubTypes.Type(value = UserAnswerDto.CodeUserAnswerDto.class, name = "CODE"),
        @JsonSubTypes.Type(value = UserAnswerDto.HtmlUserAnswerDto.class, name = "THEORY")
})
public class UserAnswerDto {
    private TaskType taskType;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestUserAnswerDto extends UserAnswerDto {
        private String userAnswer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MultiTestUserAnswerDto extends UserAnswerDto {
        private List<String> userAnswers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CodeUserAnswerDto extends UserAnswerDto {
        private String userCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HtmlUserAnswerDto extends UserAnswerDto { }
}
