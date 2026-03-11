package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.course.UserInNotInListsDto;
import ru.just.monolithmvp.dto.course.UserInNotInRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAssignmentService implements CourseEnrollmentPort {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewerRepository courseReviewerRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupCourseAssignmentRepository groupCourseAssignmentRepository;
    private final LearningGroupRepository learningGroupRepository;
    private final AppUserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;

    @Transactional
    public void enrollUnenrollStudents(Long courseId, Set<Long> idsToEnroll, Set<Long> idsToUnEnroll) {
        String actor = resolveCurrentActor();

        idsToEnroll.forEach(id -> enrollStudentToCourse(courseId, id, actor));
        idsToUnEnroll.forEach(id -> unenrollStudentFromCourse(courseId, id, actor));
    }

    @Transactional
    public void updateCourseEnrollments(Long courseId, UserInNotInRequest request) {
        validateNoOverlap(request.idsIn(), request.idsNotIn(), "Enrollment lists must be unique");
        enrollUnenrollStudents(courseId, request.idsIn(), request.idsNotIn());
    }

    @Transactional
    @Override
    public void enrollStudentToCourse(Long courseId, Long userId) {
        String actor = resolveCurrentActor();
        Course course = getCourseEntity(courseId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        enrollStudentToCourse(course.getId(), user.getId(), actor);
    }

    private void enrollStudentToCourse(Long courseId, Long userId, String actor) {
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            log.warn("Student is already enrolled in this course");
            businessEventLogger.log("course.enrollment.assign", "noop",
                    "actor", actor,
                    "courseId", courseId,
                    "userId", userId,
                    "reason", "already_enrolled");
            return;
        }

        Enrollment enrollment = new Enrollment();
        final Course course = new Course();
        course.setId(courseId);
        enrollment.setCourse(course);
        final AppUser user = new AppUser();
        user.setId(userId);
        enrollment.setUser(user);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        businessEventLogger.log("course.enrollment.assign", "success",
                "actor", actor,
                "courseId", courseId,
                "userId", userId);
    }

    public void unenrollStudentFromCourse(Long courseId, Long userId, String actor) {
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
        businessEventLogger.log("course.enrollment.unassign", "success",
                "actor", actor,
                "courseId", courseId,
                "userId", userId);
    }

    @Transactional(readOnly = true)
    public UserInNotInListsDto getEnrollmentLists(Long courseId) {
        getCourseEntity(courseId);

        List<AppUser> enrolledStudents = enrollmentRepository.findByCourseId(courseId).stream()
                .map(Enrollment::getUser)
                .toList();
        List<Long> enrolledUserIds = enrolledStudents.stream().map(AppUser::getId).toList();

        List<AppUser> notEnrolledStudents;
        if (enrolledStudents.isEmpty()) {
            notEnrolledStudents = userRepository.findAll();
        } else {
            notEnrolledStudents = userRepository.findAllByIdNotInAndActivation(enrolledUserIds, true);
        }

        return new UserInNotInListsDto(
                enrolledStudents.stream().map(userMapper::toDto).toList(),
                notEnrolledStudents.stream().map(userMapper::toDto).toList()
        );
    }

    @Transactional
    public void assignReviewerToCourse(Long courseId, Long reviewerId) {
        String actor = resolveCurrentActor();
        Course course = getCourseEntity(courseId);
        AppUser reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + reviewerId));
        if (reviewer.getRole() != Role.ADMIN) {
            throw new BadRequestException("Reviewer must be ADMIN");
        }
        CourseReviewer cr = new CourseReviewer();
        cr.setCourse(course);
        cr.setReviewer(reviewer);
        try {
            courseReviewerRepository.save(cr);
            businessEventLogger.log("course.reviewer.assign", "success",
                    "actor", actor,
                    "courseId", courseId,
                    "reviewerId", reviewerId);
        } catch (DataIntegrityViolationException e) {
            log.warn("User already reviewer on course");
            businessEventLogger.log("course.reviewer.assign", "noop",
                    "actor", actor,
                    "courseId", courseId,
                    "reviewerId", reviewerId,
                    "reason", "already_assigned");
        }
    }

    @Transactional
    public void unassignReviewerFromCourse(Long courseId, Long reviewerId) {
        String actor = resolveCurrentActor();
        courseReviewerRepository.deleteByCourseIdAndReviewerId(courseId, reviewerId);
        businessEventLogger.log("course.reviewer.unassign", "success",
                "actor", actor,
                "courseId", courseId,
                "reviewerId", reviewerId);
    }

    @Transactional
    public void assignGroupToCourse(Long courseId, UUID groupId) {
        Course course = getCourseEntity(courseId);
        String actor = resolveCurrentActor();
        LearningGroup group = learningGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (!groupCourseAssignmentRepository.existsByGroupIdAndCourseId(groupId, courseId)) {
            GroupCourseAssignment assignment = new GroupCourseAssignment();
            assignment.setGroup(group);
            assignment.setCourse(course);
            assignment.setCreatedAt(LocalDateTime.now());
            groupCourseAssignmentRepository.save(assignment);
        }

        groupMembershipRepository.findByGroupId(groupId)
                .forEach(m -> {
                    if ((m.getUser().getRole() == Role.STUDENT || m.getUser().getRole() == Role.ADMIN)
                            && !enrollmentRepository.existsByUserIdAndCourseId(m.getUser().getId(), courseId)) {
                        enrollStudentToCourse(course.getId(), m.getUser().getId(), actor);
                    }
                });
    }

    @Transactional(readOnly = true)
    public UserInNotInListsDto getReviewersToCourseLists(Long courseId) {
        final Set<Long> reviewersIds = courseReviewerRepository.findAllByCourseId(courseId).stream()
                .map(cr -> cr.getReviewer().getId()).collect(Collectors.toSet());

        final List<AppUser> admins = userRepository.findAllByRole(Role.ADMIN);

        List<UserDto> reviewers = admins.stream()
                .filter(admin -> reviewersIds.contains(admin.getId()))
                .map(userMapper::toDto).toList();
        List<UserDto> notReviewers = admins.stream()
                .filter(admin -> !reviewersIds.contains(admin.getId()))
                .map(userMapper::toDto).toList();
        return new UserInNotInListsDto(reviewers, notReviewers);
    }

    @Transactional
    public void unassignGroupFromCourse(Long courseId, UUID groupId) {
        groupCourseAssignmentRepository.deleteByGroupIdAndCourseId(groupId, courseId);
        groupMembershipRepository.findByGroupId(groupId)
                .forEach(m -> enrollmentRepository.deleteByUserIdAndCourseId(m.getUser().getId(), courseId));
    }

    @Transactional(readOnly = true)
    public boolean canReviewCourse(Long courseId, Long adminId) {
        return courseReviewerRepository.existsByCourseIdAndReviewerId(courseId, adminId);
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesThatAdminCanReview(Long adminId) {
        return courseReviewerRepository.findAllByReviewerId(adminId).stream().map(CourseReviewer::getCourse).toList();
    }

    @Transactional
    public void assignUnassignReviewers(Long courseId, Set<Long> userIdsToAssign, Set<Long> userIdsToUnassign) {
        userIdsToAssign.forEach(reviewerId -> assignReviewerToCourse(courseId, reviewerId));
        userIdsToUnassign.forEach(reviewerId -> unassignReviewerFromCourse(courseId, reviewerId));
    }

    @Transactional
    public void updateCourseReviewers(Long courseId, UserInNotInRequest request) {
        validateNoOverlap(request.idsIn(), request.idsNotIn(), "Reviewers lists must be unique");
        assignUnassignReviewers(courseId, request.idsIn(), request.idsNotIn());
    }

    @Transactional
    public void assignGroupsToCourse(Long courseId, List<UUID> groupIds) {
        groupIds.forEach(groupId -> assignGroupToCourse(courseId, groupId));
    }

    @Transactional
    public void unassignGroupsFromCourse(Long courseId, List<UUID> groupIds) {
        groupIds.forEach(groupId -> unassignGroupFromCourse(courseId, groupId));
    }

    @Transactional
    public void deleteByCourseId(Long courseId) {
        courseReviewerRepository.deleteByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Course getCourseEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    private String resolveCurrentActor() {
        return securityUtils.resolveCurrentActor();
    }

    private void validateNoOverlap(Set<Long> idsIn, Set<Long> idsNotIn, String message) {
        if (idsIn.stream().anyMatch(idsNotIn::contains)) {
            throw new BadRequestException(message);
        }
    }
}
