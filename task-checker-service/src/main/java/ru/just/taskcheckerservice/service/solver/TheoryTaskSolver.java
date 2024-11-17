package ru.just.taskcheckerservice.service.solver;

import org.springframework.stereotype.Component;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

@Component
public class TheoryTaskSolver implements TaskSolver {
    @Override
    public TaskResult solve(TaskDto taskDto) {
        return new TaskResult.TheoryTaskResult();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.THEORY;
    }
}
