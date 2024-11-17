package ru.just.taskcheckerservice.service.solver;

import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

public interface TaskSolver {
    TaskResult solve(TaskDto taskDto);
    TaskType getTaskType();
}
