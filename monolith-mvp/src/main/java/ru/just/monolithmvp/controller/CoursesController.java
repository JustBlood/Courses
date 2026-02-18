package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.common.IdsRequest;
import ru.just.monolithmvp.dto.common.UuidIdsRequest;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.program.CreateLearningProgramRequest;
import ru.just.monolithmvp.dto.program.GroupAssignmentRequest;
import ru.just.monolithmvp.dto.program.LearningProgramDto;
import ru.just.monolithmvp.dto.program.ProgramTargetType;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.ProgramService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CoursesController {
    private final CourseService courseService;
    private final ProgramService programService;


    @PostMapping("/courses")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourse(request), HttpStatus.CREATED);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> getCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }


    @PostMapping("/courses/{courseId}/assign")
    public ResponseEntity<ApiResponse> assignStudent(@PathVariable Long courseId,
                                                     @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(userId -> courseService.assignStudentToCourse(courseId, userId));
        return ResponseEntity.ok(new ApiResponse("Student assigned to course"));
    }

    @DeleteMapping("/courses/{courseId}/assign")
    public ResponseEntity<ApiResponse> unassignStudent(@PathVariable Long courseId,
                                                       @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(userId -> courseService.unassignStudentFromCourse(courseId, userId));
        return ResponseEntity.ok(new ApiResponse("Student unassigned from course"));
    }

    @PostMapping("/courses/{courseId}/reviewers")
    public ResponseEntity<ApiResponse> assignReviewer(@PathVariable Long courseId,
                                                      @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(reviewerId -> courseService.assignReviewerToCourse(courseId, reviewerId));
        return ResponseEntity.ok(new ApiResponse("Reviewer assigned to course"));
    }

    @DeleteMapping("/courses/{courseId}/reviewers")
    public ResponseEntity<ApiResponse> unassignReviewer(@PathVariable Long courseId,
                                                        @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(reviewerId -> courseService.unassignReviewerFromCourse(courseId, reviewerId));
        return ResponseEntity.ok(new ApiResponse("Reviewer unassigned from course"));
    }

    @PostMapping("/courses/{courseId}/groups/assign")
    public ResponseEntity<ApiResponse> assignGroupToCourse(@PathVariable Long courseId,
                                                           @RequestBody @Valid UuidIdsRequest request) {
        request.ids().forEach(groupId -> courseService.assignGroupToCourse(courseId, groupId));
        return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
    }

    @DeleteMapping("/courses/{courseId}/groups/assign")
    public ResponseEntity<ApiResponse> unassignGroupFromCourse(@PathVariable Long courseId,
                                                               @RequestBody @Valid UuidIdsRequest request) {
        request.ids().forEach(groupId -> courseService.unassignGroupFromCourse(courseId, groupId));
        return ResponseEntity.ok(new ApiResponse("Group unassigned from course"));
    }

    @PostMapping("/groups/{groupId}/assign")
    public ResponseEntity<ApiResponse> assignGroupToTarget(@PathVariable UUID groupId,
                                                           @RequestBody @Valid GroupAssignmentRequest request) {
        if (request.targetType() == ProgramTargetType.COURSE) {
            courseService.assignGroupToCourse(request.targetId(), groupId);
            return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
        }
        programService.assignGroupToProgram(request.targetId(), groupId);
        return ResponseEntity.ok(new ApiResponse("Group assigned to program"));
    }

    @PostMapping("/programs")
    public ResponseEntity<LearningProgramDto> createProgram(@RequestBody @Valid CreateLearningProgramRequest request) {
        return new ResponseEntity<>(programService.createProgram(request), HttpStatus.CREATED);
    }

    @GetMapping("/programs")
    public ResponseEntity<List<LearningProgramDto>> getPrograms() {
        return ResponseEntity.ok(programService.getPrograms());
    }

    @GetMapping("/programs/{programId}")
    public ResponseEntity<LearningProgramDto> getProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgram(programId));
    }

    @PostMapping("/programs/{programId}/assign")
    public ResponseEntity<ApiResponse> assignUsersToProgram(@PathVariable Long programId,
                                                            @RequestBody @Valid IdsRequest request) {
        programService.assignUsersToProgram(programId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Users assigned to program"));
    }

    @PostMapping("/programs/{programId}/groups/assign")
    public ResponseEntity<ApiResponse> assignGroupToProgram(@PathVariable Long programId,
                                                            @RequestBody @Valid UuidIdsRequest request) {
        request.ids().forEach(groupId -> programService.assignGroupToProgram(programId, groupId));
        return ResponseEntity.ok(new ApiResponse("Group assigned to program"));
    }
}
