package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CourseLearnerDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.program.LearningProgramDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.dto.student.StudentProfileDto;
import ru.just.monolithmvp.dto.student.UpdateMyProfileRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.security.SecurityUtils;
import ru.just.monolithmvp.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
@Tag(name = "Student", description = "API обучения и личного кабинета студента")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class StudentController {
    private final CourseService courseService;
    private final LearningService learningService;
    private final StatisticsService statisticsService;
    private final GroupService groupService;
    private final ProgramService programService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/my/courses")
    @Operation(summary = "Получить назначенные курсы текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список назначенных курсов", content = @Content(schema = @Schema(implementation = CourseDto.class)))
    })
    public ResponseEntity<List<CourseDto>> myCourses() {
        return ResponseEntity.ok(courseService.getMyCourses());
    }

    @GetMapping("/my/profile")
    @Operation(summary = "Получить профиль текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Профиль текущего пользователя", content = @Content(schema = @Schema(implementation = StudentProfileDto.class)))
    })
    public ResponseEntity<StudentProfileDto> myProfile() {
        Long userId = securityUtils.currentUserId();
        return ResponseEntity.ok(new StudentProfileDto(
                userService.getUser(userId),
                groupService.getUserGroups(userId)
        ));
    }

    @PatchMapping("/my/profile")
    @Operation(summary = "Обновить разрешённые поля профиля текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Профиль обновлён", content = @Content(schema = @Schema(implementation = StudentProfileDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<StudentProfileDto> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        Long userId = securityUtils.currentUserId();
        UserDto updatedUser = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(new StudentProfileDto(updatedUser, groupService.getUserGroups(userId)));
    }

    @PostMapping(value = "/my/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить/обновить аватар текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Аватар обновлен", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректный файл", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserDto> uploadMyAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateUserAvatar(securityUtils.currentUserId(), file));
    }

    @PostMapping("/my/last-visit")
    @Operation(summary = "Обновить lastVisit для текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Дата последнего визита обновлена", content = @Content(schema = @Schema(implementation = UserDto.class)))
    })
    public ResponseEntity<UserDto> updateMyLastVisit() {
        return ResponseEntity.ok(userService.updateCurrentUserLastVisit());
    }

    @GetMapping("/my/programs")
    @Operation(summary = "Получить назначенные программы обучения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список программ", content = @Content(schema = @Schema(implementation = LearningProgramDto.class)))
    })
    public ResponseEntity<List<LearningProgramDto>> myPrograms() {
        return ResponseEntity.ok(programService.getMyPrograms(securityUtils.currentUserId()));
    }

    @GetMapping("/my/programs/{programId}")
    @Operation(summary = "Получить программу обучения по ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программа найдена", content = @Content(schema = @Schema(implementation = LearningProgramDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LearningProgramDto> myProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getMyProgram(securityUtils.currentUserId(), programId));
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Получить детали курса для прохождения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Детали курса", content = @Content(schema = @Schema(implementation = CourseLearnerDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден или недоступен", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseLearnerDto> courseForLearner(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseForLearner(securityUtils.currentUserId(), courseId));
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Получить урок для прохождения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок для прохождения", content = @Content(schema = @Schema(implementation = LearnerLessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден или недоступен", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LearnerLessonDto> getLessonForLearner(@PathVariable Long lessonId) {
        return ResponseEntity.ok(learningService.getLessonForLearner(lessonId, securityUtils.currentUserId()));
    }

    @PostMapping("/lessons/{lessonId}/complete-theory")
    @Operation(summary = "Завершить теоретический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Теоретический урок завершен", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Невозможно завершить урок", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> completeTheory(@PathVariable Long lessonId) {
        return ResponseEntity.ok(learningService.completeTheoryLesson(lessonId));
    }

    @PostMapping("/lessons/{lessonId}/submit-practice")
    @Operation(summary = "Отправить ответы по практическому уроку")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ответы отправлены", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Невозможно принять попытку", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> submitPractice(@PathVariable Long lessonId,
                                                              @RequestBody PracticeSubmissionRequest request) {
        return ResponseEntity.ok(learningService.submitPractice(lessonId, request));
    }

    @GetMapping("/my/stats")
    @Operation(summary = "Получить личную статистику по курсам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Личная статистика", content = @Content(schema = @Schema(implementation = StudentCourseStatDto.class)))
    })
    public ResponseEntity<List<StudentCourseStatDto>> myStats() {
        return ResponseEntity.ok(statisticsService.myCourseStats());
    }
//
//    @GetMapping("/groups/{groupId}/users")
//    public ResponseEntity<GroupUsersDto> groupUsersById(@PathVariable UUID groupId) {
//        return ResponseEntity.ok(groupService.getGroupUsersForStudent(groupId, securityUtils.currentUserId()));
//    }
//
//    @GetMapping("/groups/users")
//    public ResponseEntity<List<GroupUsersDto>> myGroupsUsersByTitle(@RequestParam(required = false) String title) {
//        return ResponseEntity.ok(groupService.getMyGroupUsersByTitle(securityUtils.currentUserId(), title));
//    }
}
