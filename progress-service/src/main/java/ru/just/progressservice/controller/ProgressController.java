package ru.just.progressservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.progressservice.dto.CourseProgressDto;
import ru.just.progressservice.dto.ExtendedCourseProgressDto;
import ru.just.progressservice.dto.LessonDto;
import ru.just.progressservice.dto.LessonProgressDto;
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
    public ResponseEntity<Void> completeLesson(@PathVariable Long lessonId) {
        progressService.completeLesson(lessonId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/courses/{courseId}/next")
    public ResponseEntity<LessonProgressDto> getNextLesson(@PathVariable Long courseId) {
        return ResponseEntity.ok(progressService.getNextLesson(courseId));
    }
}

