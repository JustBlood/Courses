package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.course.CourseLearnerDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.program.ProgramDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.dto.student.StudentProfileDto;
import ru.just.monolithmvp.dto.user.UpdateUserRequest;
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
    private final CourseLearnerReadService courseLearnerReadService;
    private final LearningService learningService;
    private final PracticeSubmissionService practiceSubmissionService;
    private final StatisticsService statisticsService;
    private final UserService userService;
    private final ProgramService programService;
    private final SecurityUtils securityUtils;

    @GetMapping("/my/courses")
    @Operation(summary = "Получить назначенные курсы текущего пользователя", description = "Возвращает курсы, назначенные текущему пользователю, с прогрессом и уроками")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список назначенных курсов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseLearnerDto.class))))
    })
    public ResponseEntity<List<CourseLearnerDto>> myCourses() {
        return ResponseEntity.ok(courseService.getMyCourses());
    }

    @GetMapping("/my/programs")
    @Operation(summary = "Получить программы текущего пользователя", description = "Возвращает список программ обучения, назначенных текущему пользователю")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список программ пользователя", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProgramDto.class))))
    })
    public ResponseEntity<List<ProgramDto>> myPrograms() {
        return ResponseEntity.ok(programService.getMyPrograms(securityUtils.currentUserId()));
    }

    @GetMapping("/my/programs/{programId}")
    @Operation(summary = "Получить программу текущего пользователя", description = "Возвращает конкретную программу, если она назначена текущему пользователю")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программа пользователя", content = @Content(schema = @Schema(implementation = ProgramDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена или не назначена пользователю", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ProgramDto> myProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getMyProgram(securityUtils.currentUserId(), programId));
    }

    @GetMapping("/my/profile")
    @Operation(summary = "Получить профиль текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Профиль текущего пользователя", content = @Content(schema = @Schema(implementation = StudentProfileDto.class)))
    })
    public ResponseEntity<UserDto> myProfile() {
        return ResponseEntity.ok(userService.getStudentProfile(securityUtils.currentUserId()));
    }

    @PatchMapping("/my/profile")
    @Operation(summary = "Обновить разрешённые поля профиля текущего пользователя", description = "Обновляет доступные для редактирования поля профиля текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Профиль обновлён", content = @Content(schema = @Schema(implementation = StudentProfileDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<StudentProfileDto> updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateStudentProfile(securityUtils.currentUserId(), request));
    }

    @PostMapping("/my/last-visit")
    @Operation(summary = "Обновить lastVisit для текущего пользователя", description = "Обновляет отметку времени последнего посещения текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Дата последнего визита обновлена", content = @Content(schema = @Schema(implementation = UserDto.class)))
    })
    public ResponseEntity<Void> updateMyLastVisit() {
        userService.updateCurrentUserLastVisit();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/courses/{courseId}") // todo: возможно, стоит удалить этот рест совсем
    @Operation(summary = "Получить детали курса для прохождения", description = "Возвращает детальную структуру курса для текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Детали курса", content = @Content(schema = @Schema(implementation = CourseLearnerDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Пользователь не зачислен на курс", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден или недоступен", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseLearnerDto> courseForLearner(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseLearnerReadService.getCourseForLearner(securityUtils.currentUserId(), courseId));
    }

    @GetMapping("/courses/{courseId}/lessons/next")
    @Operation(summary = "Получить следующий доступный урок для прохождения", description = "Возвращает следующий доступный урок курса с учетом правил доступа")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Следующий доступный урок", content = @Content(schema = @Schema(implementation = LearnerLessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Курс недоступен по правилам прохождения", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Следующий урок отсутствует или курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LearnerLessonDto> nextLessonForLearner(@PathVariable Long courseId) {
        return ResponseEntity.ok(learningService.getNextLessonForLearner(courseId, securityUtils.currentUserId()));
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Получить урок для прохождения", description = "Возвращает данные урока для прохождения текущим пользователем")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок для прохождения", content = @Content(schema = @Schema(implementation = LearnerLessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Урок недоступен по правилам прохождения", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден или недоступен", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LearnerLessonDto> getLessonForLearner(@PathVariable Long lessonId) {
        return ResponseEntity.ok(learningService.getLessonForLearner(lessonId, securityUtils.currentUserId()));
    }

    @PostMapping("/lessons/{lessonId}/complete-theory")
    @Operation(summary = "Отметить теоретический урок как пройденный", description = "Завершает теоретический урок текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Теоретический урок отмечен как пройденный", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Невозможно отметить урок", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> completeTheoryLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(practiceSubmissionService.completeTheoryLesson(lessonId, securityUtils.currentUserId()));
    }

    @PostMapping("/lessons/{lessonId}/submit-practice")
    @Operation(summary = "Отправить ответы по практическому уроку", description = "Сохраняет отправку ответов по практическому уроку")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ответы отправлены", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Невозможно принять попытку", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> submitPractice(@PathVariable Long lessonId,
                                                              @RequestBody PracticeSubmissionRequest request) {
        return ResponseEntity.ok(practiceSubmissionService.submitPractice(lessonId, request, securityUtils.currentUserId()));
    }

    @PostMapping("/lessons/{lessonId}/start")
    @Operation(summary = "Начать прохождение урока", description = "Создает или переоткрывает попытку прохождения урока")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок начат", content = @Content(schema = @Schema(implementation = SubmissionResultDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Урок нельзя начать", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SubmissionResultDto> submitPractice(@PathVariable Long lessonId) {
        return ResponseEntity.ok(practiceSubmissionService.startLesson(lessonId, securityUtils.currentUserId()));
    }

    @GetMapping("/my/stats")
    @Operation(summary = "Получить личную статистику по курсам", description = "Возвращает сводную статистику текущего пользователя по назначенным курсам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Личная статистика", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentCourseStatDto.class))))
    })
    public ResponseEntity<List<StudentCourseStatDto>> myStats() {
        return ResponseEntity.ok(statisticsService.myCourseStats());
    }
}
