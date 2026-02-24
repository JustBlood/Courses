package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.program.CreateLearningProgramRequest;
import ru.just.monolithmvp.dto.program.LearningProgramCourseDto;
import ru.just.monolithmvp.dto.program.LearningProgramDto;
import ru.just.monolithmvp.dto.program.ProgramCourseSettingsRequest;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProgramService {
    private final LearningProgramRepository learningProgramRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final AppUserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupProgramAssignmentRepository groupProgramAssignmentRepository;
    private final LearningGroupRepository learningGroupRepository;
    private final CourseService courseService;

    @Transactional
    public LearningProgramDto createProgram(CreateLearningProgramRequest request) {
        LearningProgram program = new LearningProgram();
        program.setTitle(request.title());
        program.setDescription(request.description());
        program.setCoverFilePath(request.coverFilePath());
        program.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
        program.setDeadlineAt(request.deadlineAt());
        program.setAccessCondition(request.accessCondition());

        Map<Long, Course> courseById = validateAndLoadCourses(request.courses());

        int order = 1;
        for (ProgramCourseSettingsRequest item : request.courses()) {
            ProgramCourse programCourse = new ProgramCourse();
            programCourse.setProgram(program);
            programCourse.setCourse(courseById.get(item.courseId()));
            programCourse.setOrderIndex(order++);
            program.getCourses().add(programCourse);
        }

        program = learningProgramRepository.save(program);
        return toProgramDto(program, null);
    }

    @Transactional
    public LearningProgramDto updateProgram(Long programId, CreateLearningProgramRequest request) {
        LearningProgram program = getProgramEntity(programId);
        Map<Long, Course> courseById = validateAndLoadCourses(request.courses());

        program.setTitle(request.title());
        program.setDescription(request.description());
        program.setCoverFilePath(request.coverFilePath());
        program.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
        program.setDeadlineAt(request.deadlineAt());
        program.setAccessCondition(request.accessCondition());

        programCourseRepository.deleteByProgramId(programId);
        learningProgramRepository.flush();
        program.getCourses().clear();

        int order = 1;
        for (ProgramCourseSettingsRequest item : request.courses()) {
            ProgramCourse programCourse = new ProgramCourse();
            programCourse.setProgram(program);
            programCourse.setCourse(courseById.get(item.courseId()));
            programCourse.setOrderIndex(order++);
            program.getCourses().add(programCourse);
        }

        program = learningProgramRepository.save(program);
        return toProgramDto(program, null);
    }

    @Transactional(readOnly = true)
    public List<LearningProgramDto> getPrograms() {
        return learningProgramRepository.findAllByOrderByTitleAsc().stream()
                .map(program -> toProgramDto(program, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningProgramDto getProgram(Long programId) {
        LearningProgram program = getProgramEntity(programId);
        return toProgramDto(program, null);
    }

    @Transactional
    public void assignUsersToProgram(Long programId, List<Long> userIds) {
        LearningProgram program = getProgramEntity(programId);
        for (Long userId : userIds) {
            assignUserToProgram(program, userId);
        }
    }

    @Transactional
    public void assignGroupToProgram(Long programId, UUID groupId) {
        LearningProgram program = getProgramEntity(programId);
        LearningGroup group = learningGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (!groupProgramAssignmentRepository.existsByGroupIdAndProgramId(groupId, programId)) {
            GroupProgramAssignment assignment = new GroupProgramAssignment();
            assignment.setGroup(group);
            assignment.setProgram(program);
            assignment.setCreatedAt(LocalDateTime.now());
            groupProgramAssignmentRepository.save(assignment);
        }

        groupMembershipRepository.findByGroupId(groupId).forEach(membership -> {
            if (membership.getUser().getRole() == Role.STUDENT) {
                assignUserToProgram(program, membership.getUser().getId());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<LearningProgramDto> getMyPrograms(Long userId) {
        return programEnrollmentRepository.findByUserId(userId).stream()
                .map(ProgramEnrollment::getProgram)
                .distinct()
                .map(program -> toProgramDto(program, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningProgramDto getMyProgram(Long userId, Long programId) {
        if (!programEnrollmentRepository.existsByUserIdAndProgramId(userId, programId)) {
            throw new AccessDeniedException("Program is not assigned to current student");
        }
        LearningProgram program = getProgramEntity(programId);
        return toProgramDto(program, userId);
    }

    private LearningProgram getProgramEntity(Long programId) {
        return learningProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
    }

    private Map<Long, Course> validateAndLoadCourses(List<ProgramCourseSettingsRequest> courses) {
        List<Long> courseIds = courses.stream().map(ProgramCourseSettingsRequest::courseId).toList();
        if (new HashSet<>(courseIds).size() != courseIds.size()) {
            throw new BadRequestException("Program contains duplicate course ids");
        }

        Map<Long, Course> courseById = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(course -> courseById.put(course.getId(), course));
        List<Long> missingIds = courseIds.stream().filter(id -> !courseById.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Courses not found: " + missingIds);
        }
        return courseById;
    }

    private void assignUserToProgram(LearningProgram program, Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only STUDENT can be assigned to program");
        }

        if (!programEnrollmentRepository.existsByUserIdAndProgramId(userId, program.getId())) {
            ProgramEnrollment enrollment = new ProgramEnrollment();
            enrollment.setUser(user);
            enrollment.setProgram(program);
            enrollment.setEnrolledAt(LocalDateTime.now());
            programEnrollmentRepository.save(enrollment);
        }

        for (ProgramCourse programCourse : program.getCourses()) {
            Long courseId = programCourse.getCourse().getId();
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                courseService.assignStudentToCourse(courseId, userId);
            }
        }
    }

    private LearningProgramDto toProgramDto(LearningProgram program, Long userId) {
        boolean allPreviousCompleted = true;
        boolean allPreviousViewed = true;

        List<LearningProgramCourseDto> courseDtos = new ArrayList<>();
        for (ProgramCourse programCourse : program.getCourses()) {
            boolean completed = userId != null && isCourseCompleted(userId, programCourse.getCourse().getId());
            boolean viewed = userId != null && isCourseViewedOrPending(userId, programCourse.getCourse().getId());

            boolean available = true;
            if (userId != null) {
                boolean availableByAccessCondition = switch (program.getAccessCondition()) {
                    case ALL_OPEN -> true;
                    case PREVIOUS_COURSES_COMPLETED -> allPreviousCompleted;
                    case PREVIOUS_COURSES_VIEWED_OR_PENDING -> allPreviousViewed;
                };
                boolean availableByDeadline = isAvailableByDeadline(program, completed);
                available = availableByAccessCondition && availableByDeadline;

                allPreviousCompleted = allPreviousCompleted && completed;
                allPreviousViewed = allPreviousViewed && viewed;
            }

            courseDtos.add(new LearningProgramCourseDto(
                    programCourse.getCourse().getId(),
                    programCourse.getCourse().getTitle(),
                    programCourse.getOrderIndex(),
                    program.getDeadlineAt(),
                    Boolean.TRUE.equals(program.getBlockAfterDeadline()),
                    available,
                    completed,
                    viewed
            ));
        }

        return new LearningProgramDto(
                program.getId(),
                program.getTitle(),
                program.getDescription(),
                program.getCoverFilePath(),
                program.getAccessCondition(),
                courseDtos
        );
    }

    private boolean isAvailableByDeadline(LearningProgram program, boolean completed) {
        if (!Boolean.TRUE.equals(program.getBlockAfterDeadline())) {
            return true;
        }
        if (program.getDeadlineAt() == null) {
            return true;
        }
        if (completed) {
            return true;
        }
        return !LocalDateTime.now().isAfter(program.getDeadlineAt());
    }

    private boolean isCourseCompleted(Long userId, Long courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .map(e -> e.getCompletedAt() != null)
                .orElse(false);
    }

    private boolean isCourseViewedOrPending(Long userId, Long courseId) {
        boolean started = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .map(e -> e.getStartedAt() != null || e.getCompletedAt() != null)
                .orElse(false);
        if (started) {
            return true;
        }
        return !lessonSubmissionRepository.findByStudentIdAndLessonCourseId(userId, courseId).isEmpty();
    }
}
