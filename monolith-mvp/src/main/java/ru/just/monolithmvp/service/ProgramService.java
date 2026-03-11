package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.program.CreateLearningProgramRequest;
import ru.just.monolithmvp.dto.program.ProgramCourseDto;
import ru.just.monolithmvp.dto.program.ProgramDto;
import ru.just.monolithmvp.dto.program.ProgramGroupAssignRequest;
import ru.just.monolithmvp.dto.program.ProgramUserAssignRequest;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.LocalDateTime;
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
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final CourseEnrollmentPort courseEnrollmentPort;

    @Transactional
    public ProgramDto createProgram(CreateLearningProgramRequest request) {
        LearningProgram program = new LearningProgram();
        applyProgramFields(program, request);
        LearningProgram saved = learningProgramRepository.save(program);
        replaceProgramCourses(saved, request.courses());
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
        replaceProgramCourses(program, request.courses());
        LearningProgram saved = learningProgramRepository.save(program);

        List<Long> userIds = programEnrollmentRepository.findByProgramId(programId).stream()
                .map(pe -> pe.getUser().getId())
                .distinct()
                .toList();
        for (Long userId : userIds) {
            ensureProgramCourseEnrollmentsForUser(saved, userId);
        }

        return getProgram(programId);
    }

    @Transactional
    public void assignUsers(Long programId, Set<Long> idsIn, Set<Long> idsNotIn) {
        LearningProgram program = getProgramEntity(programId);
        validateAssignableUsers(idsIn);
        validateAssignableUsers(idsNotIn);

        for (Long userId : idsNotIn) {
            programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                    .ifPresent(enrollment -> cleanupProgramEnrollment(enrollment, true));
        }

        for (Long userId : idsIn) {
            ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
            programEnrollmentRepository.save(enrollment);
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
                assignment.setCreatedAt(LocalDateTime.now());
                groupProgramAssignmentRepository.save(assignment);
            }

            groupMembershipRepository.findByGroupId(groupId).stream()
                    .map(GroupMembership::getUser)
                    .filter(this::isLearnerRole)
                    .map(AppUser::getId)
                    .forEach(userId -> {
                        ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
                        programEnrollmentRepository.save(enrollment);
                        ensureProgramCourseEnrollmentsForUser(program, userId);
                    });
        }

        for (UUID groupId : idsNotIn) {
            groupProgramAssignmentRepository.deleteByGroupIdAndProgramId(groupId, programId);
            groupMembershipRepository.findByGroupId(groupId).stream()
                    .map(GroupMembership::getUser)
                    .map(AppUser::getId)
                    .forEach(userId -> {
                        programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                                .ifPresent(enrollment -> cleanupProgramEnrollment(enrollment, true));
                    });
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
        if (!isLearnerRole(user)) {
            return;
        }

        for (LearningProgram program : assignedPrograms) {
            ProgramEnrollment enrollment = getOrCreateProgramEnrollment(program, userId);
            programEnrollmentRepository.save(enrollment);
            ensureProgramCourseEnrollmentsForUser(program, userId);
        }
    }

    @Transactional
    public void handleGroupMembershipRemoved(UUID groupId, Long userId) {
        List<Long> programIds = groupProgramAssignmentRepository.findByGroupId(groupId).stream()
                .map(gpa -> gpa.getProgram().getId())
                .toList();
        for (Long programId : programIds) {
            programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                    .ifPresent(enrollment -> cleanupProgramEnrollment(enrollment, true));
        }
    }

    @Transactional
    public void resetProgramCourseProgress(Long programId, Long userId, Long courseId) {
        getProgramEntity(programId);
        if (!programEnrollmentRepository.existsByUserIdAndProgramId(userId, programId)) {
            throw new NotFoundException("Program enrollment not found for user: " + userId);
        }

        boolean courseInProgram = programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId).stream()
                .anyMatch(pc -> Objects.equals(pc.getCourse().getId(), courseId));
        if (!courseInProgram) {
            throw new BadRequestException("Course is not part of this program");
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found for user/course"));

        if (enrollment.getCompletedAt() != null) {
            throw new BadRequestException("Cannot reset completed course progress");
        }

        lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
        enrollment.setStartedAt(null);
        enrollment.setCompletedAt(null);
        enrollmentRepository.save(enrollment);
        onCourseProgressChanged(userId, courseId);
    }

    private boolean isUserAssignedThroughAnyGroup(Long programId, Long userId) {
        return groupMembershipRepository.findByUserId(userId).stream()
                .map(GroupMembership::getGroup)
                .map(LearningGroup::getId)
                .anyMatch(groupId -> groupProgramAssignmentRepository.existsByGroupIdAndProgramId(groupId, programId));
    }

    private ProgramEnrollment getOrCreateProgramEnrollment(LearningProgram program, Long userId) {
        return programEnrollmentRepository.findByUserIdAndProgramId(userId, program.getId())
                .orElseGet(() -> {
                    ProgramEnrollment enrollment = new ProgramEnrollment();
                    enrollment.setProgram(program);
                    AppUser user = new AppUser();
                    user.setId(userId);
                    enrollment.setUser(user);
                    enrollment.setEnrolledAt(LocalDateTime.now());
                    return enrollment;
                });
    }

    private void cleanupProgramEnrollment(ProgramEnrollment enrollment, boolean toDelete) {
        if (toDelete) {
            programEnrollmentRepository.delete(enrollment);
            return;
        }
        programEnrollmentRepository.save(enrollment);
    }

    private void ensureProgramCourseEnrollmentsForUser(LearningProgram program, Long userId) {
        ProgramDto dto = toProgramDto(program, userId);
        for (ProgramCourseDto courseDto : dto.courses()) {
            if (Boolean.TRUE.equals(courseDto.available())) {
                courseEnrollmentPort.enrollStudentToCourse(courseDto.courseId(), userId);
            }
        }
    }

    private ProgramDto toProgramDto(LearningProgram program, Long userId) {
        List<ProgramCourse> orderedCourses = program.getCourses().stream()
                .sorted(Comparator.comparing(ProgramCourse::getOrderIndex))
                .toList();

        Map<Long, Enrollment> enrollmentsByCourseId = new HashMap<>();
        if (userId != null) {
            for (ProgramCourse pc : orderedCourses) {
                enrollmentRepository.findByUserIdAndCourseId(userId, pc.getCourse().getId())
                        .ifPresent(enrollment -> enrollmentsByCourseId.put(pc.getCourse().getId(), enrollment));
            }
        }

        List<ProgramCourseDto> courseDtos = new ArrayList<>();
        boolean previousCompleted = false;
        boolean previousViewed = false;

        for (int i = 0; i < orderedCourses.size(); i++) {
            ProgramCourse pc = orderedCourses.get(i);
            Enrollment enrollment = enrollmentsByCourseId.get(pc.getCourse().getId());
            boolean viewed = enrollment != null && enrollment.getStartedAt() != null;
            boolean completed = enrollment != null && enrollment.getCompletedAt() != null;

            boolean unlockedByRule;
            if (i == 0 || program.getAccessCondition() == ProgramAccessCondition.ALL_OPEN) {
                unlockedByRule = true;
            } else if (program.getAccessCondition() == ProgramAccessCondition.PREVIOUS_COURSES_COMPLETED) {
                unlockedByRule = previousCompleted;
            } else {
                unlockedByRule = previousViewed;
            }

            boolean blockedByDeadline = Boolean.TRUE.equals(program.getBlockAfterDeadline())
                    && program.getDeadlineAt() != null
                    && LocalDateTime.now().isAfter(program.getDeadlineAt())
                    && !completed;

            boolean available = unlockedByRule && !blockedByDeadline;

            courseDtos.add(new ProgramCourseDto(
                    pc.getCourse().getId(),
                    pc.getOrderIndex(),
                    userId == null ? null : available,
                    userId == null ? null : viewed,
                    userId == null ? null : completed
            ));

            previousCompleted = completed;
            previousViewed = viewed;
        }

        Boolean completedProgram = null;
        if (userId != null) {
            completedProgram = !courseDtos.isEmpty() && courseDtos.stream().allMatch(c -> Boolean.TRUE.equals(c.completed()));
        }

        return new ProgramDto(
                program.getId(),
                program.getTitle(),
                program.getDescription(),
                program.getAccessCondition(),
                program.getDeadlineAt(),
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
        program.setTitle(request.title());
        program.setDescription(request.description());
        program.setAccessCondition(request.accessCondition() == null
                ? ProgramAccessCondition.ALL_OPEN
                : request.accessCondition());
        program.setDeadlineAt(request.deadlineAt());
        program.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
    }

    private void replaceProgramCourses(LearningProgram program, List<Long> coursesIds) {
        Map<Long, Course> coursesById = courseRepository.findAllById(coursesIds)
                .stream().collect(Collectors.toMap(Course::getId, c -> c));

        if (coursesById.size() != coursesIds.size()) {
            throw new NotFoundException("Some courses were not found");
        }

        if (program.getId() != null) {
            programCourseRepository.deleteByProgramId(program.getId());
            programCourseRepository.flush();
        }

        program.getCourses().clear();
        int i = 0;
        for (Long courseId : coursesIds) {
            ProgramCourse pc = new ProgramCourse();
            pc.setProgram(program);
            pc.setCourse(coursesById.get(courseId));
            pc.setOrderIndex(i++);
            program.getCourses().add(pc);
        }
        learningProgramRepository.saveAndFlush(program);
    }

    private void validateAssignableUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<AppUser> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new NotFoundException("Some users were not found");
        }
        boolean hasUnsupportedRole = users.stream().anyMatch(user -> !isLearnerRole(user));
        if (hasUnsupportedRole) {
            throw new BadRequestException("Only STUDENT or ADMIN can be assigned to program");
        }
    }

    private boolean isLearnerRole(AppUser user) {
        return user.getRole() == Role.STUDENT || user.getRole() == Role.ADMIN;
    }

    private <T> void validateNoOverlap(Set<T> idsIn, Set<T> idsNotIn, String message) {
        if (idsIn.stream().anyMatch(idsNotIn::contains)) {
            throw new BadRequestException(message);
        }
    }
}
