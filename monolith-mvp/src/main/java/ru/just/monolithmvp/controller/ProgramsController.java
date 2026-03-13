package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import ru.just.monolithmvp.dto.course.CourseInNotInListsDto;
import ru.just.monolithmvp.dto.course.UserInNotInListsDto;
import ru.just.monolithmvp.dto.program.CreateLearningProgramRequest;
import ru.just.monolithmvp.dto.program.ProgramCourseAssignRequest;
import ru.just.monolithmvp.dto.program.ProgramDto;
import ru.just.monolithmvp.dto.program.ProgramGroupAssignRequest;
import ru.just.monolithmvp.dto.program.ProgramUserAssignRequest;
import ru.just.monolithmvp.service.ProgramService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses/programs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Programs", description = "Администрирование программ обучения")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class ProgramsController {

    private final ProgramService programService;

    @PostMapping("")
    @Operation(summary = "Создать learning program")
    public ResponseEntity<ProgramDto> createProgram(@RequestBody @Valid CreateLearningProgramRequest request) {
        return new ResponseEntity<>(programService.createProgram(request), HttpStatus.CREATED);
    }

    @GetMapping("")
    @Operation(summary = "Получить список learning programs")
    public ResponseEntity<List<ProgramDto>> getPrograms() {
        return ResponseEntity.ok(programService.getPrograms());
    }

    @GetMapping("/{programId}")
    @Operation(summary = "Получить learning program")
    public ResponseEntity<ProgramDto> getProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgram(programId));
    }

    @PutMapping("/{programId}")
    @Operation(summary = "Обновить learning program")
    public ResponseEntity<ProgramDto> updateProgram(@PathVariable Long programId,
                                                    @RequestBody @Valid CreateLearningProgramRequest request) {
        return ResponseEntity.ok(programService.updateProgram(programId, request));
    }

    @DeleteMapping("/{programId}")
    @Operation(summary = "Удалить learning program")
    public ResponseEntity<ApiResponse> deleteProgram(@PathVariable Long programId) {
        programService.deleteProgram(programId);
        return ResponseEntity.ok(new ApiResponse("Program deleted"));
    }

    @PostMapping("/{programId}/assign")
    @Operation(summary = "Назначить/снять пользователей для learning program")
    public ResponseEntity<ApiResponse> assignUsersToProgram(@PathVariable Long programId,
                                                            @RequestBody @Valid ProgramUserAssignRequest request) {
        programService.updateProgramUsers(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program assignments updated"));
    }

    @GetMapping("/{programId}/assign")
    @Operation(summary = "Получить списки назначенных/не назначенных пользователей learning program")
    public ResponseEntity<UserInNotInListsDto> getProgramEnrollmentLists(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgramEnrollmentLists(programId));
    }

    @GetMapping("/{programId}/courses/assign")
    @Operation(summary = "Получить списки добавленных/не добавленных в learning program курсов")
    public ResponseEntity<CourseInNotInListsDto> getProgramCourseLists(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgramCourseLists(programId));
    }

    @PostMapping("/{programId}/courses/assign")
    @Operation(summary = "Добавить/удалить курсы в learning program")
    public ResponseEntity<ApiResponse> updateProgramCourseLists(@PathVariable Long programId,
                                                                @RequestBody @Valid ProgramCourseAssignRequest request) {
        programService.updateProgramCourses(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program course assignments updated"));
    }

    @PostMapping("/{programId}/groups/assign")
    @Operation(summary = "Назначить/снять группы для learning program")
    public ResponseEntity<ApiResponse> assignGroupsToProgram(@PathVariable Long programId,
                                                             @RequestBody @Valid ProgramGroupAssignRequest request) {
        programService.updateProgramGroups(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program group assignments updated"));
    }

    @PostMapping("/{programId}/users/{userId}/courses/{courseId}/reset-progress")
    @Operation(summary = "Сбросить прогресс пользователя по курсу внутри learning program")
    public ResponseEntity<ApiResponse> resetProgramCourseProgress(@PathVariable Long programId,
                                                                  @PathVariable Long userId,
                                                                  @PathVariable Long courseId) {
        programService.resetProgramCourseProgress(programId, userId, courseId);
        return ResponseEntity.ok(new ApiResponse("Program course progress reset"));
    }
}