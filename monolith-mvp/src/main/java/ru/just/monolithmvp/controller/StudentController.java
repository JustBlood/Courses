package ru.just.monolithmvp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.program.LearningProgramDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.dto.student.StudentProfileDto;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.security.SecurityUtils;
import ru.just.monolithmvp.service.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
public class StudentController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;
    private final GroupService groupService;
    private final ProgramService programService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> allCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/my/courses")
    public ResponseEntity<List<CourseDto>> myCourses() {
        return ResponseEntity.ok(courseService.getMyCourses());
    }

    @GetMapping("/my/profile")
    public ResponseEntity<StudentProfileDto> myProfile() {
        Long userId = securityUtils.currentUserId();
        return ResponseEntity.ok(new StudentProfileDto(
                userService.getUser(userId),
                groupService.getUserGroups(userId)
        ));
    }

    @PostMapping(value = "/my/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> uploadMyAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateUserAvatar(securityUtils.currentUserId(), file));
    }

    @GetMapping("/my/programs")
    public ResponseEntity<List<LearningProgramDto>> myPrograms() {
        return ResponseEntity.ok(programService.getMyPrograms(securityUtils.currentUserId()));
    }

    @GetMapping("/my/programs/{programId}")
    public ResponseEntity<LearningProgramDto> myProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getMyProgram(securityUtils.currentUserId(), programId));
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
