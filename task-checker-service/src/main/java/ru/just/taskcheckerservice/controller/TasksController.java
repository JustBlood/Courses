package ru.just.taskcheckerservice.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.error.ApiError;
import ru.just.taskcheckerservice.dto.result.TaskResult;
import ru.just.taskcheckerservice.dto.task.TaskDto;
import ru.just.taskcheckerservice.service.SolverService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/task-checker/solve")
@RequiredArgsConstructor
public class TasksController {
    private final SolverService solverService;

    @PostMapping
    public ResponseEntity<TaskResult> solveTask(@RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(solverService.solveTask(taskDto));
    }

    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<ApiError> handleNotImplementedException(NotImplementedException e) {
        return ResponseEntity.badRequest().body(new ApiError(LocalDateTime.now(), e.getMessage()));
    }
}
