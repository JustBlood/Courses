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
import ru.just.monolithmvp.dto.common.IdsRequest;
import ru.just.monolithmvp.dto.common.UuidIdsRequest;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.dto.section.SectionWithCoursesDto;
import ru.just.monolithmvp.service.CourseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Courses & Programs", description = "Администрирование курсов, уроков, назначений и программ")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class CoursesController {
    private final CourseService courseService;


    @PostMapping("")
    @Operation(summary = "Создать курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Курс создан", content = @Content(schema = @Schema(implementation = CourseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourse(request), HttpStatus.CREATED);
    }

    @GetMapping("")
    @Operation(summary = "Получить каталог курсов по разделам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Каталог курсов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseSummaryDto.class ))))
    })
    public ResponseEntity<List<SectionWithCoursesDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getCourseSummariesBySection());
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Получить курс (админ-детали)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Детали курса", content = @Content(schema = @Schema(implementation = CourseAdminDetailsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseAdminDetailsDto> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseAdminDetails(courseId));
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Обновить курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курс обновлён", content = @Content(schema = @Schema(implementation = CourseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<CourseDto> updateCourse(@PathVariable Long courseId,
                                                  @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Удалить курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курс удалён", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(new ApiResponse("Course deleted"));
    }

    @PostMapping("/{courseId}/lessons/theory")
    @Operation(summary = "Создать теоретический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Теоретический урок создан", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> createTheoryLesson(@PathVariable Long courseId,
                                                        @Valid @RequestBody CreateTheoryLessonRequest request) {
        return new ResponseEntity<>(courseService.createTheoryLesson(courseId, request), HttpStatus.CREATED);
    }

    @PostMapping("/{courseId}/lessons/practice")
    @Operation(summary = "Создать практический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Практический урок создан", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> createPracticeLesson(@PathVariable Long courseId,
                                                          @Valid @RequestBody CreatePracticeLessonRequest request) {
        return new ResponseEntity<>(courseService.createPracticeLesson(courseId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    @Operation(summary = "Получить урок курса")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок найден", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок/курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> getLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        return ResponseEntity.ok(courseService.getLesson(courseId, lessonId));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/theory")
    @Operation(summary = "Обновить теоретический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Теоретический урок обновлён", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок/курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> updateTheoryLesson(@PathVariable Long courseId,
                                                        @PathVariable Long lessonId,
                                                        @Valid @RequestBody UpdateTheoryLessonRequest request) {
        return ResponseEntity.ok(courseService.updateTheoryLesson(courseId, lessonId, request));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/practice")
    @Operation(summary = "Обновить практический урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Практический урок обновлён", content = @Content(schema = @Schema(implementation = LessonDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок/курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<LessonDto> updatePracticeLesson(@PathVariable Long courseId,
                                                          @PathVariable Long lessonId,
                                                          @Valid @RequestBody UpdatePracticeLessonRequest request) {
        return ResponseEntity.ok(courseService.updatePracticeLesson(courseId, lessonId, request));
    }

    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    @Operation(summary = "Удалить урок")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Урок удалён", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Урок/курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        courseService.deleteLesson(courseId, lessonId);
        return ResponseEntity.ok(new ApiResponse("Lesson deleted"));
    }


    @PostMapping("/{courseId}/enrollments")
    @Operation(summary = "Обновить зачисления на курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Зачисления обновлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignStudent(@PathVariable Long courseId,
                                                     @RequestBody @Valid UserInNotInRequest request) {
        if (request.idsIn().stream().anyMatch(idIn -> request.idsNotIn().contains(idIn))) {
            return ResponseEntity.badRequest().body(new ApiResponse("Enrollment lists must be unique"));
        }
        courseService.enrollUnenrollStudents(courseId, request.idsIn(), request.idsNotIn());
        return ResponseEntity.ok(new ApiResponse("Course enrollments updated"));
    }

    @GetMapping("/{courseId}/enrollments")
    @Operation(summary = "Получить списки записанных/не записанных на курс пользователей")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Списки зачисленных и доступных пользователей", content = @Content(schema = @Schema(implementation = UserInNotInListsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserInNotInListsDto> getEnrollmentLists(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getEnrollmentLists(courseId));
    }

    @PostMapping("/{courseId}/reviewers")
    @Operation(summary = "Назначить проверяющих (reviewers)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Проверяющие назначены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignReviewer(@PathVariable Long courseId,
                                                      @RequestBody @Valid UserInNotInRequest request) {
        if (request.idsIn().stream().anyMatch(idIn -> request.idsNotIn().contains(idIn))) {
            return ResponseEntity.badRequest().body(new ApiResponse("Reviewers lists must be unique"));
        }
        courseService.assignUnassignReviewers(courseId, request.idsIn(), request.idsNotIn());
        return ResponseEntity.ok(new ApiResponse("Course reviewers updated"));
    }

    @GetMapping("/{courseId}/reviewers")
    @Operation(summary = "Получить список назначенных и не назначенных проверяющих (reviewers) курса")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ОК", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserInNotInListsDto> getCourseReviewers(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getReviewersToCourseLists(courseId));
    }

    @PostMapping("/{courseId}/groups/assign")
    @Operation(summary = "Назначить группы на курс")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Группы назначены на курс", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/группа не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignGroupToCourse(@PathVariable Long courseId,
                                                            @RequestBody @Valid UuidIdsRequest request) {
        request.ids().forEach(groupId -> courseService.assignGroupToCourse(courseId, groupId));
        return ResponseEntity.ok(new ApiResponse("Group assigned to course"));
    }

    @DeleteMapping("/{courseId}/groups/assign")
    @Operation(summary = "Снять назначение групп с курса")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Назначение групп снято", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Курс/группа не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> unassignGroupFromCourse(@PathVariable Long courseId,
                                                                @RequestBody @Valid UuidIdsRequest request) {
        request.ids().forEach(groupId -> courseService.unassignGroupFromCourse(courseId, groupId));
        return ResponseEntity.ok(new ApiResponse("Group unassigned from course"));
    }

}
