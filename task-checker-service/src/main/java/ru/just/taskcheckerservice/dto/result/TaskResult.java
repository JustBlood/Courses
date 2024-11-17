package ru.just.taskcheckerservice.dto.result;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "taskType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TaskResult.TestTaskResult.class, name = "TEST"),
        @JsonSubTypes.Type(value = TaskResult.MultiTestTaskResult.class, name = "MULTI_TEST"),
        @JsonSubTypes.Type(value = TaskResult.CodeTaskResult.class, name = "CODE"),
        @JsonSubTypes.Type(value = TaskResult.TheoryTaskResult.class, name = "THEORY")
})
public abstract class TaskResult {
    private boolean correct;
    private TaskType taskType;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestTaskResult extends TaskResult {
        public TestTaskResult(boolean correct) {
            super(correct, TaskType.TEST);
        }
    }

    @Getter
    @Setter
    public static class MultiTestTaskResult extends TaskResult {
        private List<String> correctUserAnswers;
        private List<String> incorrectUserAnswers;

        public MultiTestTaskResult() {
            super(false, TaskType.MULTI_TEST);
            this.correctUserAnswers = new ArrayList<>();
            this.incorrectUserAnswers = new ArrayList<>();
        }
    }

    @Getter
    @Setter
    public static class CodeTaskResult extends TaskResult {
        private List<TaskDto.CodeTaskDto.CodeTest> correctTests;
        private List<TaskDto.CodeTaskDto.CodeTest> firstIncorrectTest;

        public CodeTaskResult() {
            super(false, TaskType.CODE);
            this.correctTests = new ArrayList<>();
            this.firstIncorrectTest = new ArrayList<>();
        }
    }

    @Getter
    @Setter
    public static class TheoryTaskResult extends TaskResult {
        public TheoryTaskResult() {
            super(true, TaskType.THEORY);
        }
    }
}
