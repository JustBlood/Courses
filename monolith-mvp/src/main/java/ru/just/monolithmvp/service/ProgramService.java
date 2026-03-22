package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.course.CourseInNotInListsDto;
import ru.just.monolithmvp.dto.course.UserInNotInListsDto;
import ru.just.monolithmvp.dto.program.*;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramService {
    private final LearningProgramRepository learningProgramRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final GroupProgramAssignmentRepository groupProgramAssignmentRepository;
    private final CourseRepository courseRepository;
    private final AppUserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final LearningGroupRepository learningGroupRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final CourseEnrollmentLifecycleService courseEnrollmentLifecycleService;
    private final CourseEnrollmentPort courseEnrollmentPort;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public ProgramDto createProgram(CreateLearningProgramRequest request) {
        LearningProgram program = new LearningProgram();
        applyProgramFields(program, request);
        LearningProgram saved = learningProgramRepository.save(program);
        return getProgram(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<ProgramDto> getPrograms() {
        return learningProgramRepository.findAll().stream()
                .map(program -> toProgramDto(program, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramDto getProgram(Long programId) {
        LearningProgram program = getProgramEntity(programId);
        return toProgramDto(program, null);
    }

    @Transactional
    public ProgramDto updateProgram(Long programId, CreateLearningProgramRequest request) {
        LearningProgram program = getProgramEntity(programId);
        applyProgramFields(program, request);
        learningProgramRepository.save(program);
        final List<ProgramCourse> existed = programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId);

        for (Long userId : getProgramEnrolledUserIds(programId)) {
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }

        return getProgram(programId);
    }

    @Transactional
    public void deleteProgram(Long programId) {
        getProgramEntity(programId);
        groupProgramAssignmentRepository.deleteByProgramId(programId);
        programEnrollmentRepository.deleteByProgramId(programId);
        programCourseRepository.deleteByProgramId(programId);
        learningProgramRepository.deleteById(programId);
    }

    @Transactional
    public void assignUsers(Long programId, Set<Long> idsIn, Set<Long> idsNotIn) {
        LearningProgram program = getProgramEntity(programId);
        validateAssignableUsers(idsIn);
        validateAssignableUsers(idsNotIn);

        for (Long userId : idsNotIn) {
            programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                    .ifPresent(pe -> courseEnrollmentLifecycleService.unassignFromProgram(programId, userId));
        }

        for (Long userId : idsIn) {
            ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
            if (enrollment.getId() == null) {
                programEnrollmentRepository.save(enrollment);
            }
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }
    }

    @Transactional
    public void updateProgramUsers(Long programId, ProgramUserAssignRequest request) {
        validateNoOverlap(request.idsIn(), request.idsNotIn(), "Program users lists must be unique");
        assignUsers(programId, request.idsIn(), request.idsNotIn());
    }

    @Transactional
    public void assignUsersToProgram(Long programId, List<Long> userIds) {
        assignUsers(programId, new LinkedHashSet<>(userIds), Set.of());
    }

    @Transactional
    public void assignGroups(Long programId, Set<UUID> idsIn, Set<UUID> idsNotIn) {
        LearningProgram program = getProgramEntity(programId);

        for (UUID groupId : idsIn) {
            LearningGroup group = learningGroupRepository.findById(groupId)
                    .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

            if (!groupProgramAssignmentRepository.existsByGroupIdAndProgramId(groupId, programId)) {
                GroupProgramAssignment assignment = new GroupProgramAssignment();
                assignment.setGroup(group);
                assignment.setProgram(program);
                assignment.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
                groupProgramAssignmentRepository.save(assignment);
            }

            groupMembershipRepository.findByGroupId(groupId).stream()
                    .map(GroupMembership::getUser)
                    .filter(this::isAssignableRole)
                    .map(AppUser::getId)
                    .forEach(userId -> {
                        ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
                        if (enrollment.getId() == null) {
                            programEnrollmentRepository.save(enrollment);
                        }
                        ensureProgramCourseEnrollmentsForUser(program, userId);
                    });
        }

        for (UUID groupId : idsNotIn) {
            groupProgramAssignmentRepository.deleteByGroupIdAndProgramId(groupId, programId);
        }
    }

    @Transactional(readOnly = true)
    public UserInNotInListsDto getProgramEnrollmentLists(Long programId) {
        getProgramEntity(programId);

        List<AppUser> enrolledUsers = programEnrollmentRepository.findByProgramId(programId).stream()
                .map(ProgramEnrollment::getUser)
                .filter(this::isAssignableRole)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(AppUser::getId, user -> user, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));

        List<Long> enrolledUserIds = enrolledUsers.stream()
                .map(AppUser::getId)
                .toList();

        List<AppUser> notEnrolledUsers;
        if (enrolledUsers.isEmpty()) {
            notEnrolledUsers = userRepository.findAll().stream()
                    .filter(this::isAssignableRole)
                    .filter(AppUser::isActivation)
                    .toList();
        } else {
            notEnrolledUsers = userRepository.findAllByIdNotInAndActivation(enrolledUserIds, true).stream()
                    .filter(this::isAssignableRole)
                    .toList();
        }

        List<UserDto> in = enrolledUsers.stream().map(userMapper::toDto).toList();
        List<UserDto> notIn = notEnrolledUsers.stream().map(userMapper::toDto).toList();
        return new UserInNotInListsDto(in, notIn);
    }

    @Transactional(readOnly = true)
    public CourseInNotInListsDto getProgramCourseLists(Long programId) {
        getProgramEntity(programId);

        List<ProgramCourse> orderedProgramCourses = programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId);
        Set<Long> inCourseIds = orderedProgramCourses.stream()
                .map(pc -> pc.getCourse().getId())
                .collect(Collectors.toSet());

        return new CourseInNotInListsDto(
                orderedProgramCourses.stream()
                        .map(ProgramCourse::getCourse)
                        .map(courseMapper::toDto)
                        .toList(),
                courseRepository.findAll().stream()
                        .filter(course -> !inCourseIds.contains(course.getId()))
                        .sorted(Comparator.comparing(Course::getId))
                        .map(courseMapper::toDto)
                        .toList()
        );
    }

    @Transactional
    public void updateProgramCourses(Long programId, ProgramCourseAssignRequest request) {
        LearningProgram program = getProgramEntity(programId);

        List<Course> existCourses = courseRepository.findAllById(request.orderedCourseIds());
        if (existCourses.size() != request.orderedCourseIds().size()) {
            throw new NotFoundException("Some courses were not found");
        }

        final List<ProgramCourse> prevProgramCourses = programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId);
        programCourseRepository.deleteByProgramId(program.getId());
        final List<Long> enrolledUserIds = getProgramEnrolledUserIds(programId);
        for (Long userId : enrolledUserIds) { // fixme: пиздец как плохо
            for (ProgramCourse programCourse : prevProgramCourses) {
                courseEnrollmentLifecycleService.unassignFromCourse(userId, programCourse.getCourse().getId(), false);
            }
        }
        program.getCourses().clear();

        List<ProgramCourse> programCourses = new ArrayList<>();
        for (int i = 0; i < request.orderedCourseIds().size(); i++) {
                final ProgramCourse programCourse = new ProgramCourse();
                Course course = new Course();
                course.setId(request.orderedCourseIds().get(i));
                programCourse.setCourse(course);
                programCourse.setProgram(program);
                programCourse.setOrderIndex(i + 1);
                programCourses.add(programCourse);
        }
        programCourseRepository.saveAllAndFlush(programCourses);

        for (Long userId : getProgramEnrolledUserIds(programId)) {
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }
    }

    @Transactional
    public void updateProgramGroups(Long programId, ProgramGroupAssignRequest request) {
        validateNoOverlap(request.idsIn(), request.idsNotIn(), "Program groups lists must be unique");
        assignGroups(programId, request.idsIn(), request.idsNotIn());
    }

    @Transactional(readOnly = true)
    public List<ProgramDto> getMyPrograms(Long userId) {
        return programEnrollmentRepository.findByUserId(userId).stream()
                .map(ProgramEnrollment::getProgram)
                .distinct()
                .map(program -> toProgramDto(program, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramDto getMyProgram(Long userId, Long programId) {
        if (!programEnrollmentRepository.existsByUserIdAndProgramId(userId, programId)) {
            throw new NotFoundException("Program not found: " + programId);
        }
        LearningProgram program = getProgramEntity(programId);
        return toProgramDto(program, userId);
    }

    @Transactional
    public void onCourseProgressChanged(Long userId, Long courseId) {
        List<LearningProgram> programs = programEnrollmentRepository.findByUserId(userId).stream()
                .map(ProgramEnrollment::getProgram)
                .filter(program -> program.getCourses().stream().anyMatch(pc -> Objects.equals(pc.getCourse().getId(), courseId)))
                .distinct()
                .toList();
        for (LearningProgram program : programs) {
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }
    }

    @Transactional
    public void handleGroupMembershipAdded(UUID groupId, Long userId) {
        List<LearningProgram> assignedPrograms = groupProgramAssignmentRepository.findByGroupId(groupId).stream()
                .map(GroupProgramAssignment::getProgram)
                .toList();

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!isAssignableRole(user)) {
            return;
        }

        for (LearningProgram program : assignedPrograms) {
            ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
            if (enrollment.getId() == null) {
                programEnrollmentRepository.save(enrollment);
            }
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }
    }

    private ProgramEnrollment getOrCreateProgramEnrollment(LearningProgram program, Long userId) {
        return programEnrollmentRepository.findByUserIdAndProgramId(userId, program.getId())
                .orElseGet(() -> {
                    ProgramEnrollment enrollment = new ProgramEnrollment();
                    enrollment.setProgram(program);
                    AppUser user = new AppUser();
                    user.setId(userId);
                    enrollment.setUser(user);
                    enrollment.setEnrolledAt(LocalDateTime.now(Clock.systemUTC()));
                    return enrollment;
                });
    }

    private void ensureProgramCourseEnrollmentsForUser(LearningProgram program, Long userId) {
        List<ResolvedProgramCourseState> courseStates = resolveProgramCourseStatesForUser(program, userId);
        for (ResolvedProgramCourseState state : courseStates) {
            if (state.available()) {
                courseEnrollmentPort.enrollStudentToCourse(state.courseId(), userId);
            }
        }
    }

    private ProgramDto toProgramDto(LearningProgram program, Long userId) {
        List<ProgramCourse> orderedCourses = getOrderedProgramCourses(program);

        final Long deadlineAt = Optional.ofNullable(program.getDeadlineDays())
                .flatMap(p -> programEnrollmentRepository.findByUserIdAndProgramId(userId, program.getId())
                        .map(pe -> pe.getEnrolledAt().plusDays(program.getDeadlineDays()).toEpochSecond(ZoneOffset.UTC)))
                .orElse(null);

        List<ProgramCourseDto> courseDtos;
        Boolean completedProgram = null;
        if (userId == null) {
            courseDtos = orderedCourses.stream()
                    .map(pc -> new ProgramCourseDto(pc.getCourse().getId(), pc.getCourse().getTitle(), pc.getCourse().getDescription(), pc.getCourse().getCoverFilePath(), null, Long.valueOf(pc.getCourse().getDeadlineDays()), pc.getOrderIndex(), null, null, null))
                    .toList();
        } else {
            List<ResolvedProgramCourseState> courseStates = resolveProgramCourseStatesForUser(program, userId);
            courseDtos = courseStates.stream()
                    .map(state -> new ProgramCourseDto(
                                state.courseId(),
                                state.title(),
                                state.description(),
                                state.coverFilePath(),
                                state.deadlineAt(),
                                state.deadlineDays(),
                                state.orderIndex(),
                                state.available(),
                                state.viewed(),
                                state.completed()
                        ))
                    .toList();
            completedProgram = !courseStates.isEmpty() && courseStates.stream().allMatch(ResolvedProgramCourseState::completed);
        }

        return new ProgramDto(
                program.getId(),
                program.getTitle(),
                program.getDescription(),
                program.getAccessCondition(),
                program.getDeadlineDays(),
                deadlineAt,
                program.getBlockAfterDeadline(),
                completedProgram,
                courseDtos
        );
    }

    private LearningProgram getProgramEntity(Long programId) {
        return learningProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
    }

    private void applyProgramFields(LearningProgram program, CreateLearningProgramRequest request) {
        program.setTitle(request.title() != null ? request.title() : program.getTitle());
        program.setDescription(request.description() != null ? request.description() : program.getDescription());
        program.setAccessCondition(request.accessCondition() == null
                ? program.getAccessCondition() != null ? program.getAccessCondition() : ProgramAccessCondition.ALL_OPEN
                : request.accessCondition());
        program.setDeadlineDays(request.deadlineDays());
        program.setBlockAfterDeadline(request.blockAfterDeadline() != null ? request.blockAfterDeadline() : program.getBlockAfterDeadline());
    }

    private List<Long> getProgramEnrolledUserIds(Long programId) {
        return programEnrollmentRepository.findByProgramId(programId).stream()
                .map(pe -> pe.getUser().getId())
                .distinct()
                .toList();
    }

    private void validateAssignableUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<AppUser> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new NotFoundException("Some users were not found");
        }
        boolean hasUnsupportedRole = users.stream().anyMatch(user -> !isAssignableRole(user));
        if (hasUnsupportedRole) {
            throw new BadRequestException("Only STUDENT or ADMIN can be assigned to program");
        }
    }

    private boolean isAssignableRole(AppUser user) {
        return user.getRole() == Role.STUDENT || user.getRole() == Role.ADMIN;
    }

    private List<ProgramCourse> getOrderedProgramCourses(LearningProgram program) {
        if (program.getId() != null) {
            return programCourseRepository.findByProgramIdOrderByOrderIndexAsc(program.getId());
        }
        return program.getCourses().stream()
                .sorted(Comparator.comparing(ProgramCourse::getOrderIndex))
                .toList();
    }

    private List<ResolvedProgramCourseState> resolveProgramCourseStatesForUser(LearningProgram program, Long userId) {
        List<ProgramCourse> orderedCourses = getOrderedProgramCourses(program);
        if (orderedCourses.isEmpty()) {
            return List.of();
        }

        Map<Long, CourseProgress> progressByCourseId = loadProgressByCourseId(userId, orderedCourses);

        List<ResolvedProgramCourseState> states = new ArrayList<>();

        boolean allPreviousCompleted = true;
        boolean previousViewed = false;

        Long deadlineAt = programEnrollmentRepository.findByUserIdAndProgramId(userId, program.getId())
                    .map(programEnrollment -> programEnrollment.getEnrolledAt().plusDays(program.getDeadlineDays()).toEpochSecond(ZoneOffset.UTC))
                    .orElse(null);

        for (int i = 0; i < orderedCourses.size(); i++) {
            ProgramCourse pc = orderedCourses.get(i);
            CourseProgress progress = progressByCourseId.get(pc.getCourse().getId());

            Long courseDeadlineAt = null;
            if (progress != null) {
                courseDeadlineAt = Optional.ofNullable(pc.getCourse().getDeadlineDays())
                        .flatMap(deadlineDays -> enrollmentRepository.findByUserIdAndCourseId(userId, pc.getCourse().getId())
                                .map(enrollment -> enrollment.getEnrolledAt().plusDays(deadlineDays).toEpochSecond(ZoneOffset.UTC)))
                        .orElse(null);
            }

            boolean viewed = progress != null && progress.getStatus() != CourseProgressStatus.NEW;
            boolean completed = progress != null && progress.getStatus() == CourseProgressStatus.COMPLETED;

            boolean unlockedByRule;
            if (i == 0 || program.getAccessCondition() == ProgramAccessCondition.ALL_OPEN) {
                unlockedByRule = true;
            } else if (program.getAccessCondition() == ProgramAccessCondition.PREVIOUS_COURSES_COMPLETED) {
                unlockedByRule = allPreviousCompleted;
            } else {
                unlockedByRule = previousViewed;
            }

            boolean blockedByDeadline = Boolean.TRUE.equals(program.getBlockAfterDeadline())
                    && program.getDeadlineDays() != null && deadlineAt != null
                    && LocalDateTime.now(Clock.systemUTC()).isAfter(LocalDateTime.ofEpochSecond(deadlineAt, 0, ZoneOffset.UTC))
                    && !completed;

            boolean available = unlockedByRule && !blockedByDeadline;
            states.add(new ResolvedProgramCourseState(
                    pc.getCourse().getId(),
                    pc.getCourse().getTitle(),
                    pc.getCourse().getDescription(),
                    pc.getCourse().getCoverFilePath(),
                    courseDeadlineAt,
                    Long.valueOf(pc.getCourse().getDeadlineDays()),
                    pc.getOrderIndex(),
                    available,
                    viewed,
                    completed
            ));

            allPreviousCompleted = allPreviousCompleted && completed;
            previousViewed = viewed;
        }

        return states;
    }

    private Map<Long, CourseProgress> loadProgressByCourseId(Long userId, List<ProgramCourse> orderedCourses) {
        List<Long> courseIds = orderedCourses.stream()
                .map(pc -> pc.getCourse().getId())
                .toList();

        return courseProgressRepository.findByUserIdAndCourseIdIn(userId, courseIds).stream()
                .collect(Collectors.toMap(
                        progress -> progress.getCourse().getId(),
                        progress -> progress,
                        (left, right) -> left
                ));
    }

    private <T> void validateNoOverlap(Collection<T> idsIn, Collection<T> idsNotIn, String message) {
        if (idsIn.stream().anyMatch(idsNotIn::contains)) {
            throw new BadRequestException(message);
        }
    }

    private <T> void validateNoDuplicates(Collection<T> ids, String message) {
        if (ids.size() != new LinkedHashSet<>(ids).size()) {
            throw new BadRequestException(message);
        }
    }

    private record ResolvedProgramCourseState(
            Long courseId,
            String title,
            String description,
            String coverFilePath,
            Long deadlineAt,
            Long deadlineDays,
            Integer orderIndex,
            boolean available,
            boolean viewed,
            boolean completed
    ) {
    }
}
