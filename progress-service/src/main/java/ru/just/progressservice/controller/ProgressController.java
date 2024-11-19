package ru.just.progressservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.progressservice.dto.*;
import ru.just.progressservice.service.ProgressService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService progressService;

    @PostMapping("/{courseId}/assign")
    public ResponseEntity<ApiResponse> assignUser(@PathVariable Long courseId, @RequestParam Long userId) {
        progressService.assignUser(courseId, userId);
        return new ResponseEntity<>(new ApiResponse("User successfully assign to course"), HttpStatus.CREATED);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseProgressDto>> getUserCourses() {
        return ResponseEntity.ok(progressService.getUserCourses());
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ExtendedCourseProgressDto> getExtendedCourseProgress(@PathVariable Long courseId) {
        return ResponseEntity.ok(progressService.getExtendedCourseProgress(courseId));
    }

    @GetMapping("/course/{courseId}/last-visited")
    public ResponseEntity<LessonDto> getLastVisitedLesson(@PathVariable Long courseId) {
        return ResponseEntity.ok(progressService.getLastVisitedLesson(courseId));
    }

    @PatchMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<TaskResult> completeLesson(@PathVariable Long lessonId, @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(progressService.completeLesson(lessonId, taskDto));
    }

    @GetMapping("/courses/{courseId}/next")
    public ResponseEntity<LessonProgressDto> getNextLesson(@PathVariable Long courseId) {
        return ResponseEntity.ok(progressService.getNextLesson(courseId));
    }
}

