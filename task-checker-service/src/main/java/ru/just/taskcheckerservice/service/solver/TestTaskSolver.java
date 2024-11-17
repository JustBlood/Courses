package ru.just.taskcheckerservice.service.solver;

import org.springframework.stereotype.Component;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

@Component
public class TestTaskSolver implements TaskSolver {

    @Override
    public TaskResult solve(TaskDto taskDto) {
        TaskDto.TestTaskDto testTaskDto = (TaskDto.TestTaskDto) taskDto;
        return new TaskResult.TestTaskResult(testTaskDto.getUserAnswer().equals(testTaskDto.getCorrectAnswer()));
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TEST;
    }
}
