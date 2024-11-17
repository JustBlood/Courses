package ru.just.taskcheckerservice.service.solver;

import org.springframework.stereotype.Component;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

@Component
public class MultiTestTaskSolver implements TaskSolver {

    @Override
    public TaskResult solve(TaskDto taskDto) {
        var multiTestTaskDto = (TaskDto.MultiTestTaskDto) taskDto;
        var result = new TaskResult.MultiTestTaskResult();
        for (var userAnswer : multiTestTaskDto.getUserAnswers()) {
            var answerContainer = multiTestTaskDto.getCorrectAnswers().contains(userAnswer)
                    ? result.getCorrectUserAnswers()
                    : result.getIncorrectUserAnswers();
            answerContainer.add(userAnswer);
        }
        result.setCorrect(result.getIncorrectUserAnswers().isEmpty());
        return result;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.MULTI_TEST;
    }
}
