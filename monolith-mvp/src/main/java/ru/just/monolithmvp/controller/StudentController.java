package ru.just.monolithmvp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.security.SecurityUtils;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.GroupService;
import ru.just.monolithmvp.service.LearningService;
import ru.just.monolithmvp.service.StatisticsService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;
    private final GroupService groupService;
    private final SecurityUtils securityUtils;

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> allCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<ApiResponse> enroll(@PathVariable Long courseId) {
        courseService.selfEnroll(courseId);
        return ResponseEntity.ok(new ApiResponse("Enrolled successfully"));
    }

    @GetMapping("/my/courses")
    public ResponseEntity<List<CourseDto>> myCourses() {
        return ResponseEntity.ok(courseService.getMyCourses());
    }

    @GetMapping("/courses/{courseId}/lessons")
    public ResponseEntity<List<LessonDto>> courseLessons(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseLessons(courseId));
    }

    @PostMapping("/lessons/{lessonId}/complete-theory")
    public ResponseEntity<SubmissionResultDto> completeTheory(@PathVariable Long lessonId) {
        return ResponseEntity.ok(learningService.completeTheoryLesson(lessonId));
    }

    @PostMapping("/lessons/{lessonId}/submit-practice")
    public ResponseEntity<SubmissionResultDto> submitPractice(@PathVariable Long lessonId,
                                                              @RequestBody PracticeSubmissionRequest request) {
        return ResponseEntity.ok(learningService.submitPractice(lessonId, request));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<List<StudentCourseStatDto>> myStats() {
        return ResponseEntity.ok(statisticsService.myCourseStats());
    }

    @GetMapping("/groups/{groupId}/users")
    public ResponseEntity<GroupUsersDto> groupUsersById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroupUsersForStudent(groupId, securityUtils.currentUserId()));
    }

    @GetMapping("/groups/users")
    public ResponseEntity<List<GroupUsersDto>> myGroupsUsersByTitle(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(groupService.getMyGroupUsersByTitle(securityUtils.currentUserId(), title));
    }
}
