package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.just.monolithmvp.config.properties.MailProperties;
import ru.just.monolithmvp.dto.user.*;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final LearningGroupRepository learningGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final SecurityUtils securityUtils;

    private static final DateTimeFormatter CSV_DT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm");

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        boolean sendInvite = request.password() == null || request.password().isBlank();

        AppUser user = new AppUser();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(sendInvite ? UUID.randomUUID().toString() : request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        user.setPhone(request.phone());
        user.setComment(request.comment());
        user.setCreatedAt(request.createdAt() == null ? LocalDateTime.now() : request.createdAt());
        user.setCreatedBy(
                request.createdBy() == null || request.createdBy().isBlank()
                        ? resolveCurrentActor()
                        : request.createdBy()
        );
        user.setLastVisit(request.lastVisit());
        user.setDeactivatedAt(request.deactivatedAt());
        user.setDeactivatedBy(request.deactivatedBy());
        user = userRepository.save(user);

        if (sendInvite) {
            sendInvite(user);
        }

        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
            throw new BadRequestException("Email already exists");
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setPhone(request.phone());
        user.setComment(request.comment());

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void setUserPassword(Long userId, SetUserPasswordRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Transactional
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        user.setRole(request.role());
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void setUsersActivation(List<Long> userIds, Boolean activation) {
        final List<AppUser> usersWithAnotherActivation = userRepository.findAllByIdInAndEnabled(userIds, !activation);
        for (AppUser user : usersWithAnotherActivation) {
            user.setEnabled(activation);
            user.setDeactivatedAt(LocalDateTime.now());
            user.setDeactivatedBy(resolveCurrentActor());
        }
        userRepository.saveAll(usersWithAnotherActivation);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        submissionRepository.deleteByStudentId(userId);
        enrollmentRepository.deleteByUserId(userId);
        programEnrollmentRepository.deleteByUserId(userId);
        passwordSetupTokenRepository.deleteByUser_Id(userId);
        groupMembershipRepository.deleteAll(groupMembershipRepository.findByUserId(userId));
        userRepository.delete(user);
    }

    @Transactional
    public void deleteUsers(List<Long> userIds) {
        userIds.forEach(this::deleteUser);
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
                            val(r, 6),
                            val(r, 14),
                            parseDateTime(val(r, 19)),
                            val(r, 20),
                            parseDateTime(val(r, 21)),
                            parseDateTime(val(r, 22)),
                            val(r, 23),
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

    private void sendInvite(AppUser user) {
        PasswordSetupToken invite = new PasswordSetupToken();
        invite.setToken(UUID.randomUUID().toString());
        invite.setUser(user);
        invite.setCreatedAt(LocalDateTime.now());
        passwordSetupTokenRepository.save(invite);

        String inviteLink = mailProperties.inviteBaseUrl() + "/set-password?token=" + invite.getToken();
        emailService.sendInvite(user.getEmail(), user.getFullName(), inviteLink);
    }

    private String resolveCurrentActor() {
        try {
            return securityUtils.currentUser().getUsername();
        } catch (Exception ex) {
            return "system";
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
}
