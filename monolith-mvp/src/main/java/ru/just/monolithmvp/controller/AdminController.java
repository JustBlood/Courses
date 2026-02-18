package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.common.IdsRequest;
import ru.just.monolithmvp.dto.common.UuidIdsRequest;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.group.CreateGroupRequest;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.dto.group.GroupUsersRequest;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.CreatePracticeLessonRequest;
import ru.just.monolithmvp.dto.lesson.CreateTheoryLessonRequest;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.program.CreateLearningProgramRequest;
import ru.just.monolithmvp.dto.program.GroupAssignmentRequest;
import ru.just.monolithmvp.dto.program.LearningProgramDto;
import ru.just.monolithmvp.dto.program.ProgramTargetType;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.user.CreateUserRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.service.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final CourseService courseService;
    private final LearningService learningService;
    private final GroupService groupService;
    private final StatisticsService statisticsService;
    private final ProgramService programService;

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

    @DeleteMapping("/users")
    public ResponseEntity<ApiResponse> deleteUsers(@RequestBody @Valid IdsRequest request) {
        userService.deleteUsers(request.ids());
        return ResponseEntity.ok(new ApiResponse("Users deleted"));
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

    @PostMapping("/groups")
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return new ResponseEntity<>(groupService.createGroup(request), HttpStatus.CREATED);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDto>> groups() {
        return ResponseEntity.ok(groupService.getGroups());
    }

    @GetMapping("/groups/{groupId}/users")
    public ResponseEntity<GroupUsersDto> groupUsersById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroupUsers(groupId));
    }

    @GetMapping("/groups/users")
    public ResponseEntity<List<GroupUsersDto>> groupUsersByTitle(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(groupService.getGroupUsersByTitle(title));
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse> addUserToGroup(@PathVariable UUID groupId,
                                                      @RequestBody @Valid GroupUsersRequest request) {
        groupService.addUsersToGroup(groupId, request.userIds());
        return ResponseEntity.ok(new ApiResponse("Users added to group"));
    }

    @DeleteMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse> removeUsersFromGroup(@PathVariable UUID groupId,
                                                            @RequestBody @Valid GroupUsersRequest request) {
        groupService.removeUsersFromGroup(groupId, request.userIds());
        return ResponseEntity.ok(new ApiResponse("Users removed from group"));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse> deleteGroup(@PathVariable UUID groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok(new ApiResponse("Group deleted"));
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

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importUsersCsv(@RequestPart("file") MultipartFile file) {
        int created = userService.importUsersFromCsv(file);
        return ResponseEntity.ok(new ApiResponse("Imported users: " + created));
    }

    @GetMapping(value = "/users/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportUsersCsv() {
        return ResponseEntity.ok(userService.exportUsersToCsv());
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

    @GetMapping("/courses/{courseId}/stats")
    public ResponseEntity<List<CourseStudentStatDto>> courseStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.courseStats(courseId));
    }

    @GetMapping(value = "/reports/summary.csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> summaryCsv() {
        return ResponseEntity.ok(statisticsService.summaryReportCsv());
    }
}
