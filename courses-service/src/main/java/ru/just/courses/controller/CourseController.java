package ru.just.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.CourseDto;
import ru.just.courses.dto.CreateCourseDto;
import ru.just.courses.dto.UpdateCourseDto;
import ru.just.courses.service.CourseService;
import ru.just.dtolib.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final String SUCCESS_OPERATION_MESSAGE_TEMPLATE = "Success %s course with id = %s";
    private final CourseService service;

    @GetMapping("/full/{courseId}")
    public ResponseEntity<CourseDto> getFullCourseById(@PathVariable("courseId") Long courseId) {
        return new ResponseEntity<>(service.getFullCourseById(courseId), HttpStatus.OK);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto> getCourseById(@PathVariable("courseId") Long courseId) {
        return new ResponseEntity<>(service.getCourseById(courseId), HttpStatus.OK);
    }

    @GetMapping("/byAuthor/{authorId}")
    public ResponseEntity<List<CourseDto>> getCoursesByAuthor(@PathVariable("authorId") Long authorId) {
        return new ResponseEntity<>(service.getCoursesByAuthor(authorId), HttpStatus.OK);
    }

    @GetMapping("/byAuthor/published/{authorId}")
    public ResponseEntity<List<CourseDto>> getPublishedCoursesByAuthor(@PathVariable("authorId") Long authorId) {
        return new ResponseEntity<>(service.getPublishedCoursesByAuthor(authorId), HttpStatus.OK);
    }

    @PatchMapping("/publish/{courseId}")
    public ResponseEntity<Void> publishCourse(@PathVariable("courseId") Long courseId, @RequestParam("isPublished") Boolean isPublished) {
        service.updateIsPublishedCourseById(courseId, isPublished);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<CourseDto> saveCourse(@Valid @RequestBody CreateCourseDto courseDto) {
        return new ResponseEntity<>(service.saveCourse(courseDto), HttpStatus.CREATED);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse> updateCourse(@PathVariable("courseId") Long courseId, @Valid @RequestBody UpdateCourseDto courseDto) {
        service.updateCourse(courseId, courseDto);
        final String message = String.format(SUCCESS_OPERATION_MESSAGE_TEMPLATE, "updating", courseId);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable("courseId") Long courseId) {
        service.deleteCourseById(courseId);
        final String message = String.format(SUCCESS_OPERATION_MESSAGE_TEMPLATE, "deleting", courseId);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
