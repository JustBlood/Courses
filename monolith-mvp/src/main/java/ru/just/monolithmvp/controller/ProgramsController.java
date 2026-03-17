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
    @Operation(summary = "Создать learning program", description = "Создает программу обучения и сохраняет порядок курсов в ней")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Программа создана", content = @Content(schema = @Schema(implementation = ProgramDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или некорректный состав курсов", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Один или несколько курсов не найдены", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ProgramDto> createProgram(@RequestBody @Valid CreateLearningProgramRequest request) {
        return new ResponseEntity<>(programService.createProgram(request), HttpStatus.CREATED);
    }

    @GetMapping("")
    @Operation(summary = "Получить список learning programs", description = "Возвращает все программы обучения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список программ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProgramDto.class))))
    })
    public ResponseEntity<List<ProgramDto>> getPrograms() {
        return ResponseEntity.ok(programService.getPrograms());
    }

    @GetMapping("/{programId}")
    @Operation(summary = "Получить learning program", description = "Возвращает программу обучения по идентификатору")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программа найдена", content = @Content(schema = @Schema(implementation = ProgramDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ProgramDto> getProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgram(programId));
    }

    @PutMapping("/{programId}")
    @Operation(summary = "Обновить learning program", description = "Обновляет данные программы и порядок курсов")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программа обновлена", content = @Content(schema = @Schema(implementation = ProgramDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или некорректный состав курсов", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа или один из курсов не найдены", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ProgramDto> updateProgram(@PathVariable Long programId,
                                                    @RequestBody @Valid CreateLearningProgramRequest request) {
        return ResponseEntity.ok(programService.updateProgram(programId, request));
    }

    @DeleteMapping("/{programId}")
    @Operation(summary = "Удалить learning program", description = "Удаляет программу, ее назначения и связи с курсами")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программа удалена", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteProgram(@PathVariable Long programId) {
        programService.deleteProgram(programId);
        return ResponseEntity.ok(new ApiResponse("Program deleted"));
    }

    @PostMapping("/{programId}/assign")
    @Operation(summary = "Назначить/снять пользователей для learning program", description = "Обновляет списки назначенных и снятых пользователей для программы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Назначения пользователей обновлены", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректные списки пользователей", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа или один из пользователей не найдены", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignUsersToProgram(@PathVariable Long programId,
                                                            @RequestBody @Valid ProgramUserAssignRequest request) {
        programService.updateProgramUsers(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program assignments updated"));
    }

    @GetMapping("/{programId}/assign")
    @Operation(summary = "Получить списки назначенных/не назначенных пользователей learning program", description = "Возвращает пользователей, уже назначенных в программу, и доступных для назначения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Списки пользователей", content = @Content(schema = @Schema(implementation = UserInNotInListsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<UserInNotInListsDto> getProgramEnrollmentLists(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgramEnrollmentLists(programId));
    }

    @GetMapping("/{programId}/courses/assign")
    @Operation(summary = "Получить списки добавленных/не добавленных в learning program курсов", description = "Возвращает курсы программы и доступные для добавления курсы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Списки курсов", content = @Content(schema = @Schema(implementation = CourseInNotInListsDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа не найдена", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<CourseInNotInListsDto> getProgramCourseLists(@PathVariable Long programId) {
        return ResponseEntity.ok(programService.getProgramCourseLists(programId));
    }

    @PostMapping("/{programId}/courses/assign")
    @Operation(summary = "Добавить/удалить курсы в learning program", description = "Обновляет состав курсов программы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Состав курсов программы обновлен", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректные списки курсов", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа или один из курсов не найдены", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> updateProgramCourseLists(@PathVariable Long programId,
                                                                @RequestBody @Valid ProgramCourseAssignRequest request) {
        programService.updateProgramCourses(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program course assignments updated"));
    }

    @PostMapping("/{programId}/groups/assign")
    @Operation(summary = "Назначить/снять группы для learning program", description = "Обновляет списки назначенных и снятых групп программы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Назначения групп обновлены", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректные списки групп", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Программа или одна из групп не найдены", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignGroupsToProgram(@PathVariable Long programId,
                                                             @RequestBody @Valid ProgramGroupAssignRequest request) {
        programService.updateProgramGroups(programId, request);
        return ResponseEntity.ok(new ApiResponse("Program group assignments updated"));
    }

}