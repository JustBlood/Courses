package ru.just.taskcheckerservice.service.solver;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;

@Component
public class DefaultTaskSolver implements TaskSolver {
    @Override
    public TaskResult solve(TaskDto taskDto) {
        throw new NotImplementedException("Данный вид задачи не поддерживается");
    }

    @Override
    public TaskType getTaskType() {
        return null;
    }
}
