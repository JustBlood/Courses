package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.lesson.CreatePracticeLessonRequest;
import ru.just.monolithmvp.dto.lesson.CreateTheoryLessonRequest;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.LessonMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewerRepository courseReviewerRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final AppUserRepository userRepository;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;
    private final SecurityUtils securityUtils;

    @Transactional
    public CourseDto createCourse(CreateCourseRequest request) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCoverFilePath(request.coverFilePath());
        course.setPassingThresholdPercent(request.passingThresholdPercent() == null ? 70 : request.passingThresholdPercent());
        course.setCreatedByAdminId(securityUtils.currentUserId());
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream().map(courseMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CourseDto getCourse(Long courseId) {
        return courseMapper.toDto(getCourseEntity(courseId));
    }

    @Transactional
    public LessonDto createTheoryLesson(Long courseId, CreateTheoryLessonRequest request) {
        Course course = getCourseEntity(courseId);
        validatePositionAvailability(courseId, request.position());

        TheoryLesson lesson = new TheoryLesson();
        lesson.setCourse(course);
        lesson.setPosition(request.position());
        lesson.setTitle(request.title());
        lesson.setContentType(request.contentType());
        lesson.setContent(request.content());
        lesson.setFullPoints(request.fullPoints() == null ? 1 : request.fullPoints());
        lesson.setPartialPoints(0);

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto createPracticeLesson(Long courseId, CreatePracticeLessonRequest request) {
        Course course = getCourseEntity(courseId);
        validatePracticeRequest(request);
        validatePositionAvailability(courseId, request.position());

        PracticeLesson lesson = new PracticeLesson();
        lesson.setCourse(course);
        lesson.setPosition(request.position());
        lesson.setTitle(request.title());
        lesson.setFullPoints(request.fullPoints() == null ? 1 : request.fullPoints());
        lesson.setPartialPoints(request.partialPoints() == null ? 0 : request.partialPoints());

        if (request.lessonType() == LessonType.PRACTICE_ASSIGNMENT) {
            lesson.setQuestionType(null);
            lesson.setQuestionText(null);
            lesson.setAssignmentPrompt(request.assignmentPrompt());
            lesson.setOptionsRaw(null);
            lesson.setCorrectAnswersRaw(null);
        } else {
            lesson.setQuestionType(request.questionType());
            lesson.setQuestionText(request.questionText());
            lesson.setAssignmentPrompt(null);
            lesson.setOptionsRaw(joinValues(request.options()));
            lesson.setCorrectAnswersRaw(joinValues(request.correctAnswers()));
        }

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public void assignStudentToCourse(Long courseId, Long userId) {
        Course course = getCourseEntity(courseId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only STUDENT can be assigned to course");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BadRequestException("Student already assigned to this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUser(user);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void selfEnroll(Long courseId) {
        assignStudentToCourse(courseId, securityUtils.currentUserId());
    }

    @Transactional
    public void unassignStudentFromCourse(Long courseId, Long userId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new NotFoundException("Enrollment not found");
        }
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    @Transactional
    public void assignReviewerToCourse(Long courseId, Long reviewerId) {
        Course course = getCourseEntity(courseId);
        AppUser reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + reviewerId));
        if (reviewer.getRole() != Role.ADMIN) {
            throw new BadRequestException("Reviewer must be ADMIN");
        }
        if (courseReviewerRepository.existsByCourseIdAndReviewerId(courseId, reviewerId)) {
            return;
        }
        ru.just.monolithmvp.model.CourseReviewer cr = new ru.just.monolithmvp.model.CourseReviewer();
        cr.setCourse(course);
        cr.setReviewer(reviewer);
        courseReviewerRepository.save(cr);
    }

    @Transactional
    public void unassignReviewerFromCourse(Long courseId, Long reviewerId) {
        courseReviewerRepository.deleteByCourseIdAndReviewerId(courseId, reviewerId);
    }

    @Transactional
    public void assignGroupToCourse(Long courseId, UUID groupId) {
        groupMembershipRepository.findByGroupId(groupId)
                .forEach(m -> {
                    if (m.getUser().getRole() == Role.STUDENT && !enrollmentRepository.existsByUserIdAndCourseId(m.getUser().getId(), courseId)) {
                        assignStudentToCourse(courseId, m.getUser().getId());
                    }
                });
    }

    @Transactional
    public void unassignGroupFromCourse(Long courseId, UUID groupId) {
        groupMembershipRepository.findByGroupId(groupId)
                .forEach(m -> enrollmentRepository.deleteByUserIdAndCourseId(m.getUser().getId(), courseId));
    }

    @Transactional(readOnly = true)
    public boolean canReviewCourse(Long courseId, Long adminId) {
        Course course = getCourseEntity(courseId);
        return course.getCreatedByAdminId().equals(adminId)
                || courseReviewerRepository.existsByCourseIdAndReviewerId(courseId, adminId);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getMyCourses() {
        Long userId = securityUtils.currentUserId();
        return enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .map(courseMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonDto> getCourseLessons(Long courseId) {
        getCourseEntity(courseId);
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Lesson getLessonEntity(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
    }

    @Transactional(readOnly = true)
    public Course getCourseEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(";;", values);
    }

    private void validatePositionAvailability(Long courseId, Integer position) {
        if (lessonRepository.existsByCourseIdAndPosition(courseId, position)) {
            throw new BadRequestException("Lesson with position %s already exists in course %s"
                    .formatted(position, courseId));
        }
    }

    private void validatePracticeRequest(CreatePracticeLessonRequest request) {
        if (request.lessonType() == LessonType.PRACTICE_ASSIGNMENT) {
            if (request.assignmentPrompt() == null || request.assignmentPrompt().isBlank()) {
                throw new BadRequestException("assignmentPrompt is required for PRACTICE_ASSIGNMENT");
            }
            return;
        }

        if (request.lessonType() != LessonType.PRACTICE_TEST) {
            throw new BadRequestException("lessonType must be PRACTICE_TEST or PRACTICE_ASSIGNMENT");
        }
        if (request.questionType() == null) {
            throw new BadRequestException("questionType is required for PRACTICE_TEST");
        }
        if (request.questionText() == null || request.questionText().isBlank()) {
            throw new BadRequestException("questionText is required for PRACTICE_TEST");
        }

        if (request.options() == null || request.options().isEmpty()) {
            throw new BadRequestException("options are required for choice questions");
        }
        if (request.correctAnswers() == null || request.correctAnswers().isEmpty()) {
            throw new BadRequestException("correctAnswers are required for choice questions");
        }

        if (request.questionType() == QuestionType.SINGLE_CHOICE && request.correctAnswers().size() != 1) {
            throw new BadRequestException("SINGLE_CHOICE must contain exactly one correct answer");
        }

        if (request.partialPoints() != null && request.fullPoints() != null && request.partialPoints() > request.fullPoints()) {
            throw new BadRequestException("partialPoints cannot be greater than fullPoints");
        }
    }
}
