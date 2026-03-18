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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.common.UuidIdsRequest;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.dto.section.SectionWithCoursesDto;
import ru.just.monolithmvp.service.CourseAssignmentService;
import ru.just.monolithmvp.service.CourseLessonAdminService;
import ru.just.monolithmvp.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Courses", description = "Администрирование курсов, уроков и назначений")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class CoursesController {
    private final CourseService courseService;
    private final CourseLessonAdminService courseLessonAdminService;
    private final CourseAssignmentService courseAssignmentService;


    @PostMapping("")
    @Operation(summary = "Создать курс", description = "Создает курс в каталоге")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Курс создан", content = @Content(schema = @Schema(implementation = CourseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или некорректные данные курса", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Раздел не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourse(request), HttpStatus.CREATED);
    }

    @GetMapping("")
    @Operation(summary = "Получить каталог курсов по разделам", description = "Возвращает список разделов с краткой информацией по курсам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Каталог курсов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SectionWithCoursesDto.class ))))
    })
    public ResponseEntity<List<SectionWithCoursesDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getCourseSummariesBySection());
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Получить курс (админ-детали)", description = "Возвращает курс с полным списком уроков")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Детали курса", content = @Content(schema = @Schema(implementation = CourseAdminDetailsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseAdminDetailsDto> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseAdminDetails(courseId));
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Обновить курс", description = "Обновляет поля курса и порядок уроков")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курс обновлён", content = @Content(schema = @Schema(implementation = CourseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или некорректные данные курса", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс или раздел не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseDto> updateCourse(@PathVariable Long courseId,
                                                  @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Удалить курс", description = "Удаляет курс и связанные сущности прохождения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курс удалён", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(new ApiResponse("Course deleted"));
    }

    @PostMapping("/{courseId}/lessons/theory")
    @Operation(summary = "Создать теоретический урок", description = "Добавляет в курс новый теоретический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Теоретический урок создан", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации урока", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> createTheoryLesson(@PathVariable Long courseId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return new ResponseEntity<>(courseLessonAdminService.createTheoryLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/{courseId}/lessons/practice")
    @Operation(summary = "Создать практический урок", description = "Добавляет в курс новый практический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Практический урок создан", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации урока", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> createPracticeLesson(@PathVariable Long courseId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return new ResponseEntity<>(courseLessonAdminService.createPracticeLesson(courseId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    @Operation(summary = "Получить урок курса", description = "Возвращает урок курса для администрирования")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок найден", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Урок не принадлежит курсу", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок или курс не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> getLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        return ResponseEntity.ok(courseLessonAdminService.getLesson(courseId, lessonId));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/theory")
    @Operation(summary = "Обновить теоретический урок", description = "Обновляет параметры теоретического урока")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Теоретический урок обновлён", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации, урок не принадлежит курсу или не является теоретическим", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> updateTheoryLesson(@PathVariable Long courseId,
                                                        @PathVariable Long lessonId,
                                                        @Valid @RequestBody UpdateTheoryLessonRequest request) {
        return ResponseEntity.ok(courseLessonAdminService.updateTheoryLesson(courseId, lessonId, request));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/practice")
    @Operation(summary = "Обновить практический урок", description = "Обновляет параметры практического урока")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Практический урок обновлён", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации, урок не принадлежит курсу или не является практическим", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> updatePracticeLesson(@PathVariable Long courseId,
                                                          @PathVariable Long lessonId,
                                                          @Valid @RequestBody UpdatePracticeLessonRequest request) {
        return ResponseEntity.ok(courseLessonAdminService.updatePracticeLesson(courseId, lessonId, request));
    }

    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    @Operation(summary = "Удалить урок", description = "Удаляет урок из курса и пересчитывает позиции")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок удалён", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Урок не принадлежит курсу", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок или курс не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        courseLessonAdminService.deleteLesson(courseId, lessonId);
        return ResponseEntity.ok(new ApiResponse("Lesson deleted"));
    }


    @PostMapping("/{courseId}/enrollments")
    @Operation(summary = "Обновить зачисления на курс", description = "Обновляет списки пользователей, добавляемых и удаляемых из зачисления на курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Зачисления обновлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации списков зачисления", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignStudent(@PathVariable Long courseId,
                                                     @RequestBody @Valid UserInNotInRequest request) {
        courseAssignmentService.updateCourseEnrollments(courseId, request);
        return ResponseEntity.ok(new ApiResponse("Course enrollments updated"));
    }

    @GetMapping("/{courseId}/enrollments")
    @Operation(summary = "Получить списки записанных/не записанных на курс пользователей", description = "Возвращает пользователей, уже зачисленных на курс, и доступных для зачисления")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Списки зачисленных и доступных пользователей", content = @Content(schema = @Schema(implementation = UserInNotInListsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserInNotInListsDto> getEnrollmentLists(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseAssignmentService.getEnrollmentLists(courseId));
    }

    @PostMapping("/{courseId}/reviewers")
    @Operation(summary = "Назначить проверяющих (reviewers)", description = "Обновляет списки назначаемых и снимаемых проверяющих по курсу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Проверяющие назначены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации списков или некорректная роль reviewer", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс или пользователь не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignReviewer(@PathVariable Long courseId,
                                                      @RequestBody @Valid UserInNotInRequest request) {
        courseAssignmentService.updateCourseReviewers(courseId, request);
        return ResponseEntity.ok(new ApiResponse("Course reviewers updated"));
    }

    @GetMapping("/{courseId}/reviewers")
    @Operation(summary = "Получить список назначенных и не назначенных проверяющих (reviewers) курса", description = "Возвращает списки назначенных и доступных для назначения администраторов-ревьюеров")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ОК", content = @Content(schema = @Schema(implementation = UserInNotInListsDto.class)))
    })
    public ResponseEntity<UserInNotInListsDto> getCourseReviewers(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseAssignmentService.getReviewersToCourseLists(courseId));
    }

    @PostMapping("/{courseId}/groups/assign")
    @Operation(summary = "Назначить группы на курс", description = "Назначает группы на курс и зачисляет участников групп")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Группы назначены на курс", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/группа не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignGroupToCourse(@PathVariable Long courseId,
                                                            @RequestBody @Valid UuidIdsRequest request) {
        courseAssignmentService.assignGroupsToCourse(courseId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
    }

    @DeleteMapping("/{courseId}/groups/assign")
    @Operation(summary = "Снять назначение групп с курса", description = "Снимает назначение групп с курса и отзывает зачисления участников")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Назначение групп снято", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/группа не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> unassignGroupFromCourse(@PathVariable Long courseId,
                                                                @RequestBody @Valid UnassignGroupFromCourseRequest request) {
        courseAssignmentService.unassignGroupsFromCourse(courseId, request.groupIds(), request.deleteProgress());
        return ResponseEntity.ok(new ApiResponse("Group unassigned from course"));
    }
}
