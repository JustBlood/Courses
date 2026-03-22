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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.common.IdsRequest;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.group.*;
import ru.just.monolithmvp.dto.program.ProgramSummaryDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.dto.user.ActivationRequest;
import ru.just.monolithmvp.dto.user.CreateUserRequest;
import ru.just.monolithmvp.dto.user.UpdateUserRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.ProgramMapper;
import ru.just.monolithmvp.service.GroupAssignmentService;
import ru.just.monolithmvp.service.GroupService;
import ru.just.monolithmvp.service.StatisticsService;
import ru.just.monolithmvp.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Users & Groups", description = "Администрирование пользователей, групп и профилей")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class UsersController {
    private final UserService userService;
    private final GroupService groupService;
    private final GroupAssignmentService groupAssignmentService;
    private final StatisticsService statisticsService;
    private final CourseMapper courseMapper;
    private final ProgramMapper programMapper;

    @PostMapping("/users")
    @Operation(summary = "Создать пользователя", description = "Создает пользователя и при необходимости отправляет ссылку для установки пароля")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Пользователь создан", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт email", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Одна или несколько групп/курсов для назначения не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @GetMapping("/users")
    @Operation(summary = "Получить список пользователей", description = "Возвращает список всех пользователей системы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список пользователей", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class))))
    })
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по идентификатору")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователь найден", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/users/{userId}/stats")
    @Operation(summary = "Получить статистику пользователя", description = "Возвращает статистику прохождения выбранного пользователя по курсам")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Статистика пользователя", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentCourseStatDto.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<List<StudentCourseStatDto>> getUserStats(@PathVariable Long userId) {
        userService.getUser(userId);
        return ResponseEntity.ok(statisticsService.userCourseStats(userId));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Обновить пользователя", description = "Обновляет профиль и учетные данные пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователь обновлен", content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт email", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<UserDto> updateUser(@PathVariable Long userId,
                                              @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PostMapping("/users/activation")
    @Operation(summary = "Массово активировать/деактивировать пользователей", description = "Изменяет статус активации для выбранных пользователей")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Статусы активации обновлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или попытка деактивации текущего администратора", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> setUsersActivation(@RequestBody @Valid ActivationRequest request) {
        userService.setUsersActivation(request.userIds(), request.activate());
        return ResponseEntity.ok(new ApiResponse("Users set activation"));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя и связанные данные прохождения")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователь удален", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Нельзя удалить текущего администратора", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(new ApiResponse("User deleted"));
    }

    @DeleteMapping("/users")
    @Operation(summary = "Массово удалить пользователей", description = "Удаляет выбранных пользователей и связанные данные")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователи удалены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или попытка удалить текущего администратора", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Один или несколько пользователей не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteUsers(@RequestBody @Valid ru.just.monolithmvp.dto.common.IdsRequest request) {
        userService.deleteUsers(request.ids());
        return ResponseEntity.ok(new ApiResponse("Users deleted"));
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Импорт пользователей из CSV", description = "Импортирует пользователей из CSV-файла и создает отсутствующие записи")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Импорт выполнен", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Некорректный CSV или ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> importUsersCsv(@RequestPart("file") MultipartFile file) {
        int created = userService.importUsersFromCsv(file);
        return ResponseEntity.ok(new ApiResponse("Imported users: " + created));
    }

    @GetMapping(value = "/users/export", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Экспорт пользователей в CSV", description = "Возвращает CSV-файл со списком пользователей")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV экспортирован", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    public ResponseEntity<String> exportUsersCsv() {
        return ResponseEntity.ok(userService.exportUsersToCsv());
    }


    @PostMapping("/groups")
    @Operation(summary = "Создать группу", description = "Создает новую учебную группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Группа создана", content = @Content(schema = @Schema(implementation = GroupDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или группа уже существует", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return new ResponseEntity<>(groupService.createGroup(request), HttpStatus.CREATED);
    }

    @PutMapping("/groups/{groupId}")
    @Operation(summary = "Обновить группу", description = "Обновляет параметры учебной группы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Группа обновлена", content = @Content(schema = @Schema(implementation = GroupDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт состава группы", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<GroupDto> updateGroup(@PathVariable UUID groupId,
                                                @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    @GetMapping("/groups")
    @Operation(summary = "Получить список групп", description = "Возвращает список всех учебных групп")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список групп", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupDto.class))))
    })
    public ResponseEntity<List<GroupWithStatDto>> groups() {
        return ResponseEntity.ok(groupService.getGroups());
    }

    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Получить полную информацию о группе")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Группа найдена", content = @Content(schema = @Schema(implementation = GroupFullInfoDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт типов групп", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа или пользователи не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<GroupFullInfoDto> getGroupFullInfoById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroupFullInfoById(groupId));
    }

    @GetMapping("/groups/users")
    @Operation(summary = "Поиск групп по title с участниками", description = "Ищет группы по title и возвращает их вместе с участниками")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Результаты поиска", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupUsersDto.class))))
    })
    public ResponseEntity<List<GroupUsersDto>> groupUsersByTitle(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(groupService.getGroupUsersByTitle(title));
    }

    @GetMapping("/groups/users/{userId}")
    @Operation(summary = "Получить список групп пользователя", description = "Возвращает список учебных групп конкретного пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список групп", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupDto.class))))
    })
    public ResponseEntity<List<GroupDto>> getUserGroups(@PathVariable Long userId) {
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @GetMapping("/groups/{groupId}/users/availableToAssign")
    @Operation(summary = "Получить список пользователей, доступных для записи в группу", description = "Возвращает список пользователей системы, которые могут быть записаны в группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список пользователей", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class))))
    })
    public ResponseEntity<List<UserDto>> getUsersWithoutGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getAvailableToAssignUsers(groupId));
    }

    @PostMapping("/groups/{groupId}/members")
    @Operation(summary = "Добавить участников в группу", description = "Добавляет выбранных пользователей в группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователи добавлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт типов групп", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа или пользователи не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> addUserToGroup(@PathVariable UUID groupId,
                                                      @RequestBody @Valid IdsRequest request) {
        groupService.addUsersToGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Users added to group"));
    }

    @DeleteMapping("/groups/{groupId}/members")
    @Operation(summary = "Удалить участников из группы", description = "Удаляет выбранных пользователей из группы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователи удалены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> removeUsersFromGroup(@PathVariable UUID groupId,
                                                            @RequestBody @Valid IdsRequest request) {
        groupService.removeUsersFromGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Users removed from group"));
    }

    @DeleteMapping("/groups/{groupId}")
    @Operation(summary = "Удалить группу", description = "Удаляет учебную группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Группа удалена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> deleteGroup(@PathVariable UUID groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok(new ApiResponse("Group deleted"));
    }

    @GetMapping("/groups/{groupId}/courses/assign")
    @Operation(summary = "Получить список курсов, доступных для назначения на группу", description = "Возвращает список курсов системы, которые могут быть записаны в группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список пользователей", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseSummaryDto.class))))
    })
    public ResponseEntity<List<CourseSummaryDto>> getAvailableToAssignCourses(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupAssignmentService.findCoursesNotAssignedToGroup(groupId).stream().map(courseMapper::toSummaryDto).toList());
    }

    @PostMapping("/groups/{groupId}/courses/assign")
    @Operation(summary = "Назначить курсы группе", description = "Назначает выбранные курсы группе")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курсы добавлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт типов групп", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа или пользователи не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignCoursesToGroup(@PathVariable UUID groupId,
                                                      @RequestBody @Valid IdsRequest request) {
        groupAssignmentService.assignCoursesToGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Courses assigned to group"));
    }

    @DeleteMapping("/groups/{groupId}/courses/assign")
    @Operation(summary = "Удалить назначения курсов с группы", description = "Удаляет назначения курсов с группы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Курсы удалены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> unassignCoursesFromGroup(@PathVariable UUID groupId,
                                                            @RequestBody @Valid IdsRequest request) {
        groupAssignmentService.unassignCoursesFromGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Courses unassigned from group"));
    }

    @GetMapping("/groups/{groupId}/programs/assign")
    @Operation(summary = "Получить список программ, доступных для назначения на группу", description = "Возвращает список программ системы, которые могут быть записаны в группу")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список программ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProgramSummaryDto.class))))
    })
    public ResponseEntity<List<ProgramSummaryDto>> getAvailableToAssignPrograms(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupAssignmentService.findProgramsNotAssignedToGroup(groupId).stream().map(programMapper::toSummaryDto).toList());
    }

    @PostMapping("/groups/{groupId}/programs/assign")
    @Operation(summary = "Назначить программы группе", description = "Назначает выбранные программы группе")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Программы добавлены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации или конфликт типов групп", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа или пользователи не найдены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> assignProgramsToGroup(@PathVariable UUID groupId,
                                                            @RequestBody @Valid IdsRequest request) {
        groupAssignmentService.assignProgramsToGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Programs assigned to group"));
    }

    @DeleteMapping("/groups/{groupId}/programs/assign")
    @Operation(summary = "Удалить назначения курсов с группы", description = "Удаляет назначения курсов с группы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Пользователи удалены", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> unassignProgramsFromGroup(@PathVariable UUID groupId,
                                                                @RequestBody @Valid IdsRequest request) {
        groupAssignmentService.unassignProgramsFromGroup(groupId, request.ids());
        return ResponseEntity.ok(new ApiResponse("Programs unassigned from group"));
    }
}
