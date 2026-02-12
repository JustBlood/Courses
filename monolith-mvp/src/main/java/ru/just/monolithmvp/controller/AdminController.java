package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.CreatePracticeLessonRequest;
import ru.just.monolithmvp.dto.lesson.CreateTheoryLessonRequest;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.user.CreateUserRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.LearningService;
import ru.just.monolithmvp.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final CourseService courseService;
    private final LearningService learningService;

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(new ApiResponse("User deleted"));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourse(request), HttpStatus.CREATED);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> getCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PostMapping("/courses/{courseId}/lessons/theory")
    public ResponseEntity<LessonDto> createTheoryLesson(@PathVariable Long courseId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return new ResponseEntity<>(courseService.createTheoryLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/courses/{courseId}/lessons/practice")
    public ResponseEntity<LessonDto> createPracticeLesson(@PathVariable Long courseId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return new ResponseEntity<>(courseService.createPracticeLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/courses/{courseId}/assign")
    public ResponseEntity<ApiResponse> assignStudent(@PathVariable Long courseId,
                                                     @RequestParam Long userId) {
        courseService.assignStudentToCourse(courseId, userId);
        return ResponseEntity.ok(new ApiResponse("Student assigned to course"));
    }

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<PendingSubmissionDto>> pendingReviews() {
        return ResponseEntity.ok(learningService.getPendingReviews());
    }

    @PostMapping("/reviews/{submissionId}")
    public ResponseEntity<SubmissionResultDto> reviewOpenAnswer(@PathVariable Long submissionId,
                                                                @RequestBody ReviewOpenSubmissionRequest request) {
        return ResponseEntity.ok(learningService.reviewOpenSubmission(submissionId, request));
    }
}
