package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.common.IdsRequest;
import ru.just.monolithmvp.dto.course.CourseAdminDetailsDto;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CourseEnrollmentListsDto;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.lesson.CreatePracticeLessonRequest;
import ru.just.monolithmvp.dto.lesson.CreateTheoryLessonRequest;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.ProgramService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CoursesController {
    private final CourseService courseService;
    private final ProgramService programService;


    @PostMapping("")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourse(request), HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<List<CourseSummaryDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getCourseSummaries());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseAdminDetailsDto> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseAdminDetails(courseId));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseDto> updateCourse(@PathVariable Long courseId,
                                                  @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(new ApiResponse("Course deleted"));
    }

    @PostMapping("/{courseId}/lessons/theory")
    public ResponseEntity<LessonDto> createTheoryLesson(@PathVariable Long courseId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return new ResponseEntity<>(courseService.createTheoryLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/{courseId}/lessons/practice")
    public ResponseEntity<LessonDto> createPracticeLesson(@PathVariable Long courseId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return new ResponseEntity<>(courseService.createPracticeLesson(courseId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<LessonDto> getLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        return ResponseEntity.ok(courseService.getLesson(courseId, lessonId));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/theory")
    public ResponseEntity<LessonDto> updateTheoryLesson(@PathVariable Long courseId,
                                                        @PathVariable Long lessonId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return ResponseEntity.ok(courseService.updateTheoryLesson(courseId, lessonId, request));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/practice")
    public ResponseEntity<LessonDto> updatePracticeLesson(@PathVariable Long courseId,
                                                          @PathVariable Long lessonId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return ResponseEntity.ok(courseService.updatePracticeLesson(courseId, lessonId, request));
    }

    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse> deleteLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        courseService.deleteLesson(courseId, lessonId);
        return ResponseEntity.ok(new ApiResponse("Lesson deleted"));
    }


    @PostMapping("/{courseId}/assign")
    public ResponseEntity<ApiResponse> assignStudent(@PathVariable Long courseId,
                                                     @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(userId -> courseService.assignStudentToCourse(courseId, userId));
        return ResponseEntity.ok(new ApiResponse("Student assigned to course"));
    }

    @DeleteMapping("/{courseId}/assign")
    public ResponseEntity<ApiResponse> unassignStudent(@PathVariable Long courseId,
                                                       @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(userId -> courseService.unassignStudentFromCourse(courseId, userId));
        return ResponseEntity.ok(new ApiResponse("Student unassigned from course"));
    }

    @GetMapping("/{courseId}/enrollments/lists")
    public ResponseEntity<CourseEnrollmentListsDto> getEnrollmentLists(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getEnrollmentLists(courseId));
    }

    @PostMapping("/{courseId}/enrollments")
    public ResponseEntity<ApiResponse> enrollStudents(@PathVariable Long courseId,
                                                      @RequestBody @Valid IdsRequest request) {
        courseService.enrollStudentsToCourse(courseId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Students enrolled to course"));
    }

    @DeleteMapping("/{courseId}/enrollments")
    public ResponseEntity<ApiResponse> unenrollStudents(@PathVariable Long courseId,
                                                        @RequestBody @Valid IdsRequest request) {
        courseService.unenrollStudentsFromCourse(courseId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Students unenrolled from course"));
    }

    @PostMapping("/{courseId}/reviewers")
    public ResponseEntity<ApiResponse> assignReviewer(@PathVariable Long courseId,
                                                      @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(reviewerId -> courseService.assignReviewerToCourse(courseId, reviewerId));
        return ResponseEntity.ok(new ApiResponse("Reviewer assigned to course"));
    }

    @DeleteMapping("/{courseId}/reviewers")
    public ResponseEntity<ApiResponse> unassignReviewer(@PathVariable Long courseId,
                                                        @RequestBody @Valid IdsRequest request) {
        request.ids().forEach(reviewerId -> courseService.unassignReviewerFromCourse(courseId, reviewerId));
        return ResponseEntity.ok(new ApiResponse("Reviewer unassigned from course"));
    }

//    @PostMapping("/{courseId}/groups/assign")
//    public ResponseEntity<ApiResponse> assignGroupToCourse(@PathVariable Long courseId,
//                                                           @RequestBody @Valid UuidIdsRequest request) {
//        request.ids().forEach(groupId -> courseService.assignGroupToCourse(courseId, groupId));
//        return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
//    }
//
//    @DeleteMapping("/{courseId}/groups/assign")
//    public ResponseEntity<ApiResponse> unassignGroupFromCourse(@PathVariable Long courseId,
//                                                               @RequestBody @Valid UuidIdsRequest request) {
//        request.ids().forEach(groupId -> courseService.unassignGroupFromCourse(courseId, groupId));
//        return ResponseEntity.ok(new ApiResponse("Group unassigned from course"));
//    }

//    @PostMapping("/groups/{groupId}/assign")
//    public ResponseEntity<ApiResponse> assignGroupToTarget(@PathVariable UUID groupId,
//                                                           @RequestBody @Valid GroupAssignmentRequest request) {
//        if (request.targetType() == ProgramTargetType.COURSE) {
//            courseService.assignGroupToCourse(request.targetId(), groupId);
//            return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
//        }
//        programService.assignGroupToProgram(request.targetId(), groupId);
//        return ResponseEntity.ok(new ApiResponse("Group assigned to program"));
//    }
//
//    @PostMapping("/programs")
//    public ResponseEntity<LearningProgramDto> createProgram(@RequestBody @Valid CreateLearningProgramRequest request) {
//        return new ResponseEntity<>(programService.createProgram(request), HttpStatus.CREATED);
//    }
//
//    @GetMapping("/programs")
//    public ResponseEntity<List<LearningProgramDto>> getPrograms() {
//        return ResponseEntity.ok(programService.getPrograms());
//    }
//
//    @GetMapping("/programs/{programId}")
//    public ResponseEntity<LearningProgramDto> getProgram(@PathVariable Long programId) {
//        return ResponseEntity.ok(programService.getProgram(programId));
//    }
//
//    @PostMapping("/programs/{programId}/assign")
//    public ResponseEntity<ApiResponse> assignUsersToProgram(@PathVariable Long programId,
//                                                            @RequestBody @Valid IdsRequest request) {
//        programService.assignUsersToProgram(programId, request.ids());
//        return ResponseEntity.ok(new ApiResponse("Users assigned to program"));
//    }
//
//    @PostMapping("/programs/{programId}/groups/assign")
//    public ResponseEntity<ApiResponse> assignGroupToProgram(@PathVariable Long programId,
//                                                            @RequestBody @Valid UuidIdsRequest request) {
//        request.ids().forEach(groupId -> programService.assignGroupToProgram(programId, groupId));
//        return ResponseEntity.ok(new ApiResponse("Group assigned to program"));
//    }
}
