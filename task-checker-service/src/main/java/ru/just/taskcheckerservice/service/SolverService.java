package ru.just.taskcheckerservice.service;

import org.springframework.stereotype.Service;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.model.TaskType;
import ru.just.taskcheckerservice.service.solver.DefaultTaskSolver;
import ru.just.taskcheckerservice.service.solver.TaskSolver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SolverService {
    private final Map<TaskType, TaskSolver> solvers;
    private final DefaultTaskSolver defaultTaskSolver;

    public SolverService(List<TaskSolver> solvers, DefaultTaskSolver defaultTaskSolver) {
        this.solvers = solvers.stream()
                .collect(Collectors.toMap(TaskSolver::getTaskType, solver -> solver));
        this.defaultTaskSolver = defaultTaskSolver;
    }

    public TaskResult solveTask(TaskDto taskDto) {
        return solvers.getOrDefault(taskDto.getTaskType(), defaultTaskSolver).solve(taskDto);
    }
}
