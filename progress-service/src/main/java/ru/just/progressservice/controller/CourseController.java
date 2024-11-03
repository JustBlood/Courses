package ru.just.progressservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.progressservice.service.CourseService;

@RestController
@RequestMapping(value = "/api/v1/progress/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @PostMapping("/{courseId}/assign")
    public ResponseEntity<ApiResponse> assignUser(@PathVariable Long courseId, @RequestParam Long userId) {
        courseService.assignUser(courseId, userId);
        return new ResponseEntity<>(new ApiResponse("User successfully assign to course"), HttpStatus.CREATED);
    }
}
