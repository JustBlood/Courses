package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.just.monolithmvp.config.properties.MailProperties;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.student.StudentProfileDto;
import ru.just.monolithmvp.dto.user.CreateUserRequest;
import ru.just.monolithmvp.dto.user.UpdateUserRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final LearningGroupRepository learningGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupService groupService;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter CSV_DT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm");

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String actor = resolveCurrentActor();
        try {
            if (userRepository.existsByEmail(request.email())) {
                throw new BadRequestException("Email already exists");
            }

            boolean sendInvite = request.password() == null || request.password().isBlank();

            AppUser user = new AppUser();
            user.setFullName(request.fullName());
            user.setEmail(request.email());
            user.setPasswordHash(passwordEncoder.encode(sendInvite ? UUID.randomUUID().toString() : request.password()));
            user.setRole(request.role());
            user.setAvatarFilePath(fileStorageService.normalizeStoredPath(request.avatarFilePath()));
            user.setActivation(true);
            user.setEnabled(!sendInvite);
            user.setPhone(request.phone());
            user.setSnils(request.snils());
            user.setComment(request.comment());
            user.setCreatedAt(request.createdAt() == null ? LocalDateTime.now(Clock.systemUTC()) : LocalDateTime.ofEpochSecond(request.createdAt(), 0, ZoneOffset.UTC));
            user.setCreatedBy(
                    request.createdBy() == null || request.createdBy().isBlank()
                            ? actor
                            : request.createdBy()
            );
            user.setLastVisit(request.lastVisit() == null ? null : LocalDateTime.ofEpochSecond(request.lastVisit(), 0, ZoneOffset.UTC));
            user.setDeactivatedAt(request.deactivatedAt() == null ? null : LocalDateTime.ofEpochSecond(request.deactivatedAt(), 0, ZoneOffset.UTC));
            user.setDeactivatedBy(request.deactivatedBy());
            user = userRepository.save(user);

            applyInitialAssignments(user, request.groupIds(), request.courseIds());

            if (sendInvite) {
                sendPasswordLink(user);
            }

            businessEventLogger.log("user.create", "success",
                    "actor", actor,
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "invite", sendInvite);

            return userMapper.toDto(user);
        } catch (RuntimeException ex) {
            businessEventLogger.log("user.create", "failure",
                    "actor", actor,
                    "email", request.email(),
                    "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        String actor = resolveCurrentActor();
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));

            if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
                throw new BadRequestException("Email already exists");
            }

            user.setFullName(request.fullName() != null ? request.fullName() : user.getFullName());
            user.setEmail(request.email() != null ? request.email() : user.getEmail());
            user.setRole(request.role() != null ? request.role() : user.getRole());
            user.setPhone(request.phone() != null ? request.phone() : user.getPhone());
            user.setSnils(request.snils() != null ? request.snils() : user.getSnils());
            user.setComment(request.comment() != null ? request.comment() : user.getComment());
            if (request.avatarFilePath() != null) {
                String oldAvatarPath = user.getAvatarFilePath();
                String newAvatarPath = fileStorageService.normalizeStoredPath(request.avatarFilePath());
                if (!fileStorageService.isFileExistsByRelativePath(newAvatarPath)) {
                    throw new BadRequestException("New avatar path is not valid or file does not exists.");
                }
                if (!Objects.equals(oldAvatarPath, newAvatarPath)) {
                    fileStorageService.deleteIfExists(oldAvatarPath);
                    user.setAvatarFilePath(newAvatarPath);
                }
            }
            if (request.password() != null && !request.password().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(request.password()));
                user.setActivation(true);
                user.setEnabled(true);
            }

            AppUser savedUser = userRepository.save(user);
            businessEventLogger.log("user.update", "success",
                    "actor", actor,
                    "userId", savedUser.getId(),
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole());
            return userMapper.toDto(savedUser, groupService.getUserGroups(userId));
        } catch (RuntimeException ex) {
            businessEventLogger.log("user.update", "failure",
                    "actor", actor,
                    "userId", userId,
                    "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional
    public UserDto updateMyProfile(Long userId, UpdateUserRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (request.fullName() == null
                && request.email() == null
                && request.role() == null
                && request.avatarFilePath() == null
                && request.phone() == null
                && request.snils() == null
                && request.comment() == null
                && request.password() == null) {
            throw new BadRequestException("At least one field must be provided for profile update");
        }

        Role currentRole = securityUtils.currentUser().getRole();
        if (currentRole == Role.STUDENT && (request.role() != null || request.comment() != null)) {
            throw new AccessDeniedException("Access forbidden for user.");
        }

        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw new BadRequestException("fullName must not be blank");
            }
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null) {
            if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.email());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.avatarFilePath() != null) {
            String oldAvatarPath = user.getAvatarFilePath();
            String newAvatarPath = fileStorageService.normalizeStoredPath(request.avatarFilePath());
            if (!fileStorageService.isFileExistsByRelativePath(newAvatarPath)) {
                throw new BadRequestException("New avatar path is not valid or file does not exists.");
            }
            if (!Objects.equals(oldAvatarPath, newAvatarPath)) {
                fileStorageService.deleteIfExists(oldAvatarPath);
                user.setAvatarFilePath(newAvatarPath);
            }
        }

        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }

        if (request.snils() != null) {
            user.setSnils(request.snils().trim());
        }

        if (request.comment() != null) {
            user.setComment(request.comment().trim());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setActivation(true);
            user.setEnabled(true);
        }

        return userMapper.toDto(userRepository.save(user), groupService.getUserGroups(userId));
    }

    @Transactional(readOnly = true)
    public UserDto getStudentProfile(Long userId) {
        return hideCommentForStudent(getUser(userId));
    }

    @Transactional
    public StudentProfileDto updateStudentProfile(Long userId, UpdateUserRequest request) {
        UserDto updatedUser = updateMyProfile(userId, request);
        return new StudentProfileDto(hideCommentForStudent(updatedUser), groupService.getUserGroups(userId));
    }

    @Transactional
    public void setUsersActivation(List<Long> userIds, Boolean activation) {
        final List<AppUser> usersWithAnotherActivation = userRepository.findAllByIdInAndActivation(userIds, !activation);
        Long currentUserId = tryResolveCurrentUserId();
        for (AppUser user : usersWithAnotherActivation) {
            if (currentUserId != null && Objects.equals(user.getId(), currentUserId) && !activation) {
                throw new BadRequestException("Admin cannot deactivate self");
            }
            user.setActivation(activation);
            if (activation) {
                user.setDeactivatedAt(null);
                user.setDeactivatedBy(null);
            } else {
                user.setDeactivatedAt(LocalDateTime.now(Clock.systemUTC()));
                user.setDeactivatedBy(resolveCurrentActor());
            }
        }
        userRepository.saveAll(usersWithAnotherActivation);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        final List<AppUser> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    final List<GroupDto> userGroups = groupService.getUserGroups(user.getId());
                    return userMapper.toDto(user, userGroups);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        final List<GroupDto> userGroups = groupService.getUserGroups(userId);
        return userMapper.toDto(user, userGroups);
    }

    @Transactional
    public void deleteUser(Long userId) {
        Long currentUserId = tryResolveCurrentUserId();
        if (currentUserId != null && Objects.equals(currentUserId, userId)) {
            throw new BadRequestException("Admin cannot delete self");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        fileStorageService.deleteIfExists(user.getAvatarFilePath());

        submissionRepository.deleteByStudentId(userId);
        enrollmentRepository.deleteByUserId(userId);
        passwordSetupTokenRepository.deleteByUser_Id(userId);
        groupMembershipRepository.deleteAll(groupMembershipRepository.findByUserId(userId));
        userRepository.delete(user);
    }

    @Transactional
    public void deleteUsers(List<Long> userIds) {
        Long currentUserId = tryResolveCurrentUserId();
        if (currentUserId != null && userIds.contains(currentUserId)) {
            throw new BadRequestException("Admin cannot delete self");
        }
        userIds.forEach(this::deleteUser);
    }

    @Transactional
    public void updateLastVisit(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        user.setLastVisit(LocalDateTime.now(Clock.systemUTC()));
        userRepository.save(user);
    }

    @Transactional
    public void updateCurrentUserLastVisit() {
        Long userId = securityUtils.currentUserId();
        updateLastVisit(userId);
    }

    @Transactional
    public int importUsersFromCsv(MultipartFile file) {
        try {
            String raw = new String(file.getBytes());
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(';')
                    .setQuote('"')
                    .setIgnoreSurroundingSpaces(true)
                    .build();

            int created = 0;
            try (CSVParser parser = CSVParser.parse(new StringReader(raw), format)) {
                for (CSVRecord r : parser) {
                    if (r.size() < 24) {
                        continue;
                    }

                    String roleRaw = val(r, 0).toUpperCase(Locale.ROOT);
                    if (!"ADMIN".equals(roleRaw) && !"STUDENT".equals(roleRaw)) {
                        continue;
                    }

                    String email = val(r, 1);
                    if (email.isBlank() || userRepository.existsByEmail(email)) {
                        continue;
                    }

                    CreateUserRequest req = new CreateUserRequest(
                            val(r, 3),
                            email,
                            ru.just.monolithmvp.model.Role.valueOf(roleRaw),
                            null,
                            val(r, 6),
                            null,
                            val(r, 14),
                            parseDateTime(val(r, 19)).toEpochSecond(ZoneOffset.UTC),
                            val(r, 20),
                            parseDateTime(val(r, 21)).toEpochSecond(ZoneOffset.UTC),
                            parseDateTime(val(r, 22)).toEpochSecond(ZoneOffset.UTC),
                            val(r, 23),
                            null,
                            null,
                            null
                    );

                    UserDto createdUser = createUser(req);
                    Long userId = createdUser.id();

                    bindTypedGroup(userId, GroupType.COMPANY, val(r, 9));
                    bindTypedGroup(userId, GroupType.DEPARTMENT, val(r, 11));
                    bindTypedGroup(userId, GroupType.POSITION, val(r, 13));
                    bindGeneralGroups(userId, val(r, 15));
                    created++;
                }
            }
            return created;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV file", e);
        }
    }

    @Transactional(readOnly = true)
    public String exportUsersToCsv() {
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setQuote('"').build();
        try (StringWriter out = new StringWriter(); CSVPrinter printer = new CSVPrinter(out, format)) {
            for (AppUser u : userRepository.findAll()) {
                Optional<LearningGroup> company = findTypedGroup(u.getId(), GroupType.COMPANY);
                Optional<LearningGroup> department = findTypedGroup(u.getId(), GroupType.DEPARTMENT);
                Optional<LearningGroup> position = findTypedGroup(u.getId(), GroupType.POSITION);
                List<LearningGroup> generalGroups = findGeneralGroups(u.getId());

                printer.printRecord(
                        u.getRole().name().toLowerCase(Locale.ROOT),
                        n(u.getEmail()),
                        n(""),
                        n(u.getFullName()),
                        n(u.getId()),
                        n(""),
                        n(u.getPhone()),
                        "",
                        company.map(g -> n(g.getId())).orElse(""),
                        company.map(LearningGroup::getTitle).orElse(""),
                        department.map(g -> n(g.getId())).orElse(""),
                        department.map(LearningGroup::getTitle).orElse(""),
                        position.map(g -> n(g.getId())).orElse(""),
                        position.map(LearningGroup::getTitle).orElse(""),
                        n(u.getComment()),
                        joinTitles(generalGroups),
                        joinIds(generalGroups),
                        "",
                        "",
                        formatDateTime(u.getCreatedAt()),
                        n(u.getCreatedBy()),
                        formatDateTime(u.getLastVisit()),
                        formatDateTime(u.getDeactivatedAt()),
                        n(u.getDeactivatedBy())
                );
            }
            printer.flush();
            return out.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export CSV", e);
        }
    }

    public void sendPasswordLink(AppUser user) {
        PasswordSetupToken invite = new PasswordSetupToken();
        invite.setToken(UUID.randomUUID().toString());
        invite.setUser(user);
        invite.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        passwordSetupTokenRepository.save(invite);

        String inviteLink = mailProperties.inviteBaseUrl() + "/set-password?token=" + invite.getToken();
        emailService.sendPasswordLink(user.getEmail(), user.getFullName(), inviteLink);
    }

    private String resolveCurrentActor() {
        return securityUtils.resolveCurrentActor();
    }

    private Long tryResolveCurrentUserId() {
        try {
            return securityUtils.currentUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private String val(CSVRecord r, int idx) {
        String v = idx < r.size() ? r.get(idx) : "";
        return v == null ? "" : v.trim();
    }

    private String n(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), CSV_DT_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(CSV_DT_FORMAT);
    }

    private void bindTypedGroup(Long userId, GroupType type, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        LearningGroup group = learningGroupRepository.findByTitleAndType(title.trim(), type)
                .orElseGet(() -> {
                    LearningGroup g = new LearningGroup();
                    g.setTitle(title.trim());
                    g.setType(type);
                    return learningGroupRepository.save(g);
                });
        if (!groupMembershipRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            GroupMembership membership = new GroupMembership();
            membership.setGroup(group);
            membership.setUser(userRepository.getReferenceById(userId));
            groupMembershipRepository.save(membership);
        }
    }

    private void bindGeneralGroups(Long userId, String titlesRaw) {
        if (titlesRaw == null || titlesRaw.isBlank()) {
            return;
        }
        for (String rawTitle : titlesRaw.split(",")) {
            String title = rawTitle.trim();
            if (title.isBlank()) {
                continue;
            }
            bindTypedGroup(userId, GroupType.GENERAL, title);
        }
    }

    private Optional<LearningGroup> findTypedGroup(Long userId, GroupType type) {
        return groupMembershipRepository.findByUserIdAndGroup_Type(userId, type)
                .map(GroupMembership::getGroup);
    }

    private void applyInitialAssignments(AppUser user, List<UUID> groupIds, List<Long> courseIds) {
        assignUserToGroups(user, groupIds);
        assignUserToCourses(user, courseIds);
    }

    private void assignUserToGroups(AppUser user, List<UUID> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }

        List<UUID> uniqueGroupIds = new ArrayList<>(new LinkedHashSet<>(groupIds));
        if (uniqueGroupIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("groupIds must not contain null values");
        }

        List<LearningGroup> groups = learningGroupRepository.findAllById(uniqueGroupIds);
        Set<UUID> foundGroupIds = groups.stream().map(LearningGroup::getId).collect(java.util.stream.Collectors.toSet());
        List<UUID> missingGroupIds = uniqueGroupIds.stream().filter(id -> !foundGroupIds.contains(id)).toList();
        if (!missingGroupIds.isEmpty()) {
            throw new NotFoundException("Groups not found: " + missingGroupIds);
        }

        long nonGeneralTypeCount = groups.stream()
                .map(LearningGroup::getType)
                .filter(type -> type != GroupType.GENERAL)
                .distinct()
                .count();
        long nonGeneralGroupsCount = groups.stream().filter(group -> group.getType() != GroupType.GENERAL).count();
        if (nonGeneralTypeCount != nonGeneralGroupsCount) {
            throw new BadRequestException("Only one group per typed category is allowed");
        }

        for (LearningGroup group : groups) {
            if (groupMembershipRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
                continue;
            }
            GroupMembership membership = new GroupMembership();
            membership.setGroup(group);
            membership.setUser(user);
            groupMembershipRepository.save(membership);
        }
    }

    private void assignUserToCourses(AppUser user, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }

        List<Long> uniqueCourseIds = new ArrayList<>(new LinkedHashSet<>(courseIds));
        if (uniqueCourseIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("courseIds must not contain null values");
        }

        List<Course> courses = courseRepository.findAllById(uniqueCourseIds);
        Set<Long> foundCourseIds = courses.stream().map(Course::getId).collect(java.util.stream.Collectors.toSet());
        List<Long> missingCourseIds = uniqueCourseIds.stream().filter(id -> !foundCourseIds.contains(id)).toList();
        if (!missingCourseIds.isEmpty()) {
            throw new NotFoundException("Courses not found: " + missingCourseIds);
        }

        for (Course course : courses) {
            if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                continue;
            }
            Enrollment enrollment = new Enrollment();
            enrollment.setUser(user);
            enrollment.setCourse(course);
            enrollment.setEnrolledAt(LocalDateTime.now(Clock.systemUTC()));
            enrollmentRepository.save(enrollment);
        }
    }

    private List<LearningGroup> findGeneralGroups(Long userId) {
        List<LearningGroup> result = new ArrayList<>();
        for (GroupMembership m : groupMembershipRepository.findByUserId(userId)) {
            if (m.getGroup().getType() == GroupType.GENERAL) {
                result.add(m.getGroup());
            }
        }
        return result;
    }

    private String joinTitles(List<LearningGroup> groups) {
        return groups.stream().map(LearningGroup::getTitle).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private String joinIds(List<LearningGroup> groups) {
        return groups.stream().map(g -> g.getId().toString()).reduce((a, b) -> a + "," + b).orElse("");
    }

    private UserDto hideCommentForStudent(UserDto user) {
        if (user.role() != Role.STUDENT) {
            return user;
        }
        return new UserDto(
                user.id(),
                user.fullName(),
                user.email(),
                user.role(),
                user.activation(),
                user.enabled(),
                user.phone(),
                user.snils(),
                null,
                user.avatarFilePath(),
                user.createdAt(),
                user.createdBy(),
                user.lastVisit(),
                user.deactivatedAt(),
                user.deactivatedBy(),
                user.groups()
        );
    }

}
