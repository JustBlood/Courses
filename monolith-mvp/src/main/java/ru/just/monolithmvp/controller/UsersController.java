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
import ru.just.monolithmvp.dto.group.CreateGroupRequest;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.dto.group.GroupUsersRequest;
import ru.just.monolithmvp.dto.user.*;
import ru.just.monolithmvp.service.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {
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

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long userId,
                                              @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PostMapping("/users/{userId}/password")
    public ResponseEntity<ApiResponse> setUserPassword(@PathVariable Long userId,
                                                       @Valid @RequestBody SetUserPasswordRequest request) {
        userService.setUserPassword(userId, request);
        return ResponseEntity.ok(new ApiResponse("Password updated"));
    }

    @PostMapping("/users/activation")
    public ResponseEntity<ApiResponse> setUsersActivation(@RequestBody @Valid ActivationRequest request) {
        userService.setUsersActivation(request.userIds(), request.activate());
        return ResponseEntity.ok(new ApiResponse("Users set activation"));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long userId,
                                                  @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(userId, request));
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

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importUsersCsv(@RequestPart("file") MultipartFile file) {
        int created = userService.importUsersFromCsv(file);
        return ResponseEntity.ok(new ApiResponse("Imported users: " + created));
    }

    @GetMapping(value = "/users/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportUsersCsv() {
        return ResponseEntity.ok(userService.exportUsersToCsv());
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
}
