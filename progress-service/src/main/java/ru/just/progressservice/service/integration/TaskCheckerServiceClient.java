package ru.just.progressservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.just.progressservice.dto.TaskDto;
import ru.just.progressservice.dto.TaskResult;

@FeignClient(value = "task-checker-service", path = "/api/v1/task-checker/solve")
public interface TaskCheckerServiceClient {
    @PostMapping
    TaskResult solveLesson(@RequestBody TaskDto taskDto);
}
