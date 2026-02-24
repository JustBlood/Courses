package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.LessonMapper;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewerRepository courseReviewerRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupCourseAssignmentRepository groupCourseAssignmentRepository;
    private final LearningGroupRepository learningGroupRepository;
    private final AppUserRepository userRepository;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final SectionService sectionService;
    private final BusinessEventLogger businessEventLogger;

    @Transactional
    public CourseDto createCourse(CreateCourseRequest request) {
        Course course = new Course();
        applyCourseFields(course, request);
        course.setCreatedByAdminId(securityUtils.currentUserId());
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional
    public CourseDto createCourseInSection(Long sectionId, CreateCourseRequest request) {
        CreateCourseRequest requestWithSection = new CreateCourseRequest(
                request.title(),
                request.description(),
                request.authorFullName(),
                request.coverFilePath(),
                request.passingThresholdPercent(),
                request.deadlineDays(),
                request.lessonsFreeOrder(),
                request.allowContinueAfterFail(),
                request.blockAfterDeadline(),
                request.keepAccessAfterDeadline(),
                request.includeInOverallStats(),
                sectionId,
                request.lessonIdToPosition()
        );
        return createCourse(requestWithSection);
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> getCourseSummaries() {
        return courseRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((Course c) -> c.getSection(), Comparator.nullsLast(
                                Comparator.comparing(Section::getPriority)
                                        .thenComparing(Section::getId)
                        ))
                        .thenComparing(Course::getId)
                )
                .map(this::toCourseSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> getReviewerCourseSummaries(Long adminId) {
        return findCoursesThatAdminCanReview(adminId).stream()
                .map(this::toCourseSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> getMyReviewerCourseSummaries() {
        return getReviewerCourseSummaries(securityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public CourseAdminDetailsDto getCourseAdminDetails(Long courseId) {
        Course course = getCourseEntity(courseId);
        List<LessonDto> lessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(lessonMapper::toDto)
                .toList();
        return new CourseAdminDetailsDto(courseMapper.toDto(course), lessons);
    }

    @Transactional
    public CourseDto updateCourse(Long courseId, CreateCourseRequest request) {
        Course course = getCourseEntity(courseId);
        setLessonsToNewPositionsIfNeeded(courseId, request);
        applyCourseFields(course, request);
        return courseMapper.toDto(courseRepository.save(course));
    }

    private void setLessonsToNewPositionsIfNeeded(Long courseId, CreateCourseRequest request) {
        if (CollectionUtils.isEmpty(request.lessonIdToPosition())) {
            return;
        }
        final List<Lesson> lessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
        Set<Long> existingIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        if (!existingIds.equals(request.lessonIdToPosition().keySet())) {
            throw new BadRequestException("lessonIdToPosition must contain all course lessons");
        }
        List<Integer> sortedPositions = request.lessonIdToPosition().values().stream().sorted().toList();
        List<Integer> expectedPositions = IntStream.rangeClosed(1, request.lessonIdToPosition().size())
                .boxed()
                .toList();
        if (!sortedPositions.equals(expectedPositions)) {
            throw new BadRequestException("lessonIdToPosition must contain valid positions in correct order");
        }
        int offset = lessons.size();
        lessons.forEach(lesson -> lesson.setPosition(lesson.getPosition() + offset)); // todo: refactor. Очень непроизводительно.
        lessonRepository.saveAllAndFlush(lessons);
        lessons.forEach(lesson -> {
            lesson.setPosition(request.lessonIdToPosition().get(lesson.getId()));
        });
        lessonRepository.saveAll(lessons);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = getCourseEntity(courseId);
        submissionRepository.deleteByLesson_Course_Id(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseReviewerRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    @Transactional
    public LessonDto createTheoryLesson(Long courseId, CreateTheoryLessonRequest request) {
        Course course = getCourseEntity(courseId);
        final int position = nextLessonPosition(courseId);

        TheoryLesson lesson = new TheoryLesson();
        lesson.setCourse(course);
        lesson.setPosition(position);
        applyCommonLessonFields(lesson, request.title(), request.description(), request.coverFilePath(),
                request.requiresPreviousCompleted(), request.openForAccess(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());
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
        final Integer lessonPosition = nextLessonPosition(courseId);

        PracticeLesson lesson = new PracticeLesson();
        lesson.setCourse(course);
        lesson.setPosition(lessonPosition);
        applyCommonLessonFields(lesson, request.title(), request.description(), request.coverFilePath(),
                request.requiresPreviousCompleted(), request.openForAccess(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());

        lesson.setPassingThresholdPercent(request.passingThresholdPercent() == null ? 100 : request.passingThresholdPercent());
        lesson.setEvaluateByCorrectCount(Boolean.TRUE.equals(request.evaluateByCorrectCount()));
        lesson.setRandomQuestionCount(request.randomQuestionCount());
        lesson.setShuffleOnEveryAttempt(Boolean.TRUE.equals(request.shuffleOptions()));
        lesson.setShowQuestionStatus(request.showQuestionStatus() == null || request.showQuestionStatus());
        lesson.setShowCorrectAnswersAfterCompletion(Boolean.TRUE.equals(request.showCorrectAnswers()));
        lesson.setLessonType(request.lessonType());

        lesson.setFullPoints(request.fullPoints() == null ? 1 : request.fullPoints());
        lesson.setPartialPoints(request.partialPoints() == null ? 0 : request.partialPoints());

        if (request.questions() != null && !request.questions().isEmpty()) {
            applyQuestionPool(lesson, request.questions());
        }

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public LessonDto getLesson(Long courseId, Long lessonId) {
        final Lesson lesson = getLessonEntity(lessonId);
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }
        return lessonMapper.toDto(lesson);
    }

    @Transactional
    public LessonDto updateTheoryLesson(Long courseId, Long lessonId, CreateTheoryLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof TheoryLesson lesson)) {
            throw new BadRequestException("Lesson is not theory");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        applyCommonLessonFields(lesson, request.title(), request.description(), request.coverFilePath(),
                request.requiresPreviousCompleted(), request.openForAccess(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());
        lesson.setContentType(request.contentType());
        lesson.setContent(request.content());
        lesson.setFullPoints(request.fullPoints() == null ? 1 : request.fullPoints());
        lesson.setPartialPoints(0);

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto updatePracticeLesson(Long courseId, Long lessonId, CreatePracticeLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof PracticeLesson lesson)) {
            throw new BadRequestException("Lesson is not practice");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        validatePracticeRequest(request);
        applyCommonLessonFields(lesson, request.title(), request.description(), request.coverFilePath(),
                request.requiresPreviousCompleted(), request.openForAccess(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());
        lesson.setPassingThresholdPercent(request.passingThresholdPercent() == null ? 100 : request.passingThresholdPercent());
        lesson.setEvaluateByCorrectCount(Boolean.TRUE.equals(request.evaluateByCorrectCount()));
        lesson.setRandomQuestionCount(request.randomQuestionCount());
        lesson.setShuffleOnEveryAttempt(Boolean.TRUE.equals(request.shuffleOptions()));
        lesson.setShowQuestionStatus(request.showQuestionStatus() == null || request.showQuestionStatus());
        lesson.setShowCorrectAnswersAfterCompletion(Boolean.TRUE.equals(request.showCorrectAnswers()));
        lesson.setLessonType(request.lessonType());

        lesson.setFullPoints(request.fullPoints() == null ? lesson.getFullPoints() : request.fullPoints());
        lesson.setPartialPoints(request.partialPoints() == null ? lesson.getPartialPoints() : request.partialPoints());

        if (request.questions() != null && !request.questions().isEmpty()) {
            applyQuestionPool(lesson, request.questions());
        }

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long courseId, Long lessonId) {
        getCourseEntity(courseId);
        Lesson lesson = getLessonEntity(lessonId);
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }
        final List<Lesson> lessonsToUpdatePosition = lessonRepository.findByCourse_IdAndPositionGreaterThan(courseId, lesson.getPosition());
        submissionRepository.deleteByLessonId(lessonId);
        practiceQuestionRepository.deleteAllByLessonId(lesson.getId());
        lessonRepository.delete(lesson);
        lessonRepository.flush();
        lessonsToUpdatePosition.forEach(nextLesson -> nextLesson.setPosition(nextLesson.getPosition() - 1));
        lessonRepository.saveAll(lessonsToUpdatePosition);
    }

    @Transactional(readOnly = true)
    public CourseLearnerDto getCourseForLearner(Long userId, Long courseId) {
        validateStudentEnrolled(userId, courseId);
        Course course = getCourseEntity(courseId);
        List<LearnerLessonSummaryDto> lessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(lesson -> new LearnerLessonSummaryDto(
                        lesson.getId(),
                        lesson.getPosition(),
                        lesson.getTitle(),
                        lesson.getLessonType()
                ))
                .toList();

        return new CourseLearnerDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverFilePath(),
                course.getDeadlineDays(),
                lessons
        );
    }

    @Transactional
    public void assignStudentToCourse(Long courseId, Long userId) {
        String actor = resolveCurrentActor();
        Course course = getCourseEntity(courseId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            log.warn("Student already assigned to this course");
            businessEventLogger.log("course.enrollment.assign", "noop",
                    "actor", actor,
                    "courseId", courseId,
                    "userId", userId,
                    "reason", "already_assigned");
            return;
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUser(user);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        businessEventLogger.log("course.enrollment.assign", "success",
                "actor", actor,
                "courseId", courseId,
                "userId", userId);
    }

    @Transactional
    public void unassignStudentFromCourse(Long courseId, Long userId) {
        String actor = resolveCurrentActor();
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new NotFoundException("User not assigned to course");
        }
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
        businessEventLogger.log("course.enrollment.unassign", "success",
                "actor", actor,
                "courseId", courseId,
                "userId", userId);
    }

    @Transactional(readOnly = true)
    public CourseEnrollmentListsDto getEnrollmentLists(Long courseId) {
        getCourseEntity(courseId);

        List<AppUser> allStudents = userRepository.findAllByRole(Role.STUDENT);
        Set<Long> enrolledUserIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(enrollment -> enrollment.getUser().getId())
                .collect(Collectors.toSet());

        List<AppUser> enrolledStudents = allStudents.stream()
                .filter(user -> enrolledUserIds.contains(user.getId()))
                .sorted(Comparator.comparing(AppUser::getId))
                .toList();

        List<AppUser> notEnrolledStudents = allStudents.stream()
                .filter(user -> !enrolledUserIds.contains(user.getId()))
                .sorted(Comparator.comparing(AppUser::getId))
                .toList();

        return new CourseEnrollmentListsDto(
                enrolledStudents.stream().map(userMapper::toDto).toList(),
                notEnrolledStudents.stream().map(userMapper::toDto).toList()
        );
    }

    @Transactional
    public void enrollStudentsToCourse(Long courseId, List<Long> userIds) {
        getCourseEntity(courseId);
        validateStudentsExist(userIds);
        userIds.forEach(userId -> assignStudentToCourse(courseId, userId));
    }

    @Transactional
    public void unenrollStudentsFromCourse(Long courseId, List<Long> userIds) {
        getCourseEntity(courseId);
        validateStudentsExist(userIds);
        userIds.forEach(userId -> unassignStudentFromCourse(courseId, userId));
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
        if (courseReviewerRepository.existsByCourseIdAndReviewerId(courseId, reviewerId)) {
            log.warn("User already reviewer on course");
            businessEventLogger.log("course.reviewer.assign", "noop",
                    "actor", actor,
                    "courseId", courseId,
                    "reviewerId", reviewerId,
                    "reason", "already_assigned");
            return;
        }
        CourseReviewer cr = new CourseReviewer();
        cr.setCourse(course);
        cr.setReviewer(reviewer);
        courseReviewerRepository.save(cr);
        businessEventLogger.log("course.reviewer.assign", "success",
                "actor", actor,
                "courseId", courseId,
                "reviewerId", reviewerId);
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
                        assignStudentToCourse(courseId, m.getUser().getId());
                    }
                });
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
    public List<PracticeQuestion> getPracticeQuestionForLesson(Long lessonId) {
        return practiceQuestionRepository.findByLessonIdOrderByQuestionIndexAsc(lessonId);
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

    private void validateStudentEnrolled(Long userId, Long courseId) {
        if (!isUserEnrolled(userId, courseId)) {
            throw new BadRequestException("Student is not enrolled in this course");
        }
    }

    private void applyCourseFields(Course course, CreateCourseRequest request) {
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setAuthorFullName(request.authorFullName());
        course.setCoverFilePath(request.coverFilePath());
        course.setPassingThresholdPercent(request.passingThresholdPercent() == null ? 100 : request.passingThresholdPercent());
        course.setDeadlineDays(request.deadlineDays());
        course.setLessonsFreeOrder(Boolean.TRUE.equals(request.lessonsFreeOrder()));
        course.setAllowContinueAfterFail(Boolean.TRUE.equals(request.allowContinueAfterFail()));
        course.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
        course.setKeepAccessAfterDeadline(Boolean.TRUE.equals(request.keepAccessAfterDeadline()));
        course.setIncludeInOverallStats(request.includeInOverallStats() == null || request.includeInOverallStats());
        course.setSection(request.sectionId() == null ? null : sectionService.getSectionEntity(request.sectionId()));
    }

    private void applyCommonLessonFields(Lesson lesson,
                                         String title,
                                         String description,
                                         String coverFilePath,
                                         Boolean requiresPreviousCompleted,
                                         Boolean openForAccess,
                                         Boolean stopLesson,
                                         Boolean blockedDuringAttempt,
                                         Integer attemptLimit,
                                         Integer timeLimitMinutes) {
        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setStopLesson(Boolean.TRUE.equals(stopLesson));
        lesson.setBlockedDuringAttempt(blockedDuringAttempt == null || blockedDuringAttempt);
        lesson.setAttemptLimit(attemptLimit);
        lesson.setTimeLimitMinutes(timeLimitMinutes);
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(";;", values);
    }

    private int nextLessonPosition(Long courseId) {
        Lesson last = lessonRepository.findFirstByCourse_IdOrderByPositionDesc(courseId);
        return last == null ? 1 : last.getPosition() + 1;
    }

    private void validatePositionAvailability(Long courseId, Integer position) {
        if (lessonRepository.existsByCourseIdAndPosition(courseId, position)) {
            throw new BadRequestException("Lesson with position %s already exists in course %s"
                    .formatted(position, courseId));
        }
    }

    private void validatePositionAvailabilityForUpdate(Long courseId, Long lessonId, Integer position) {
        if (lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, position, lessonId)) {
            throw new BadRequestException("Lesson with position %s already exists in course %s"
                    .formatted(position, courseId));
        }
    }

    private void applyQuestionPool(PracticeLesson lesson, List<PracticeQuestionRequest> questions) {
        List<PracticeQuestion> mapped = new ArrayList<>();
        for (PracticeQuestionRequest q : questions) {
            PracticeQuestion entity = new PracticeQuestion();
            entity.setLesson(lesson);
            entity.setQuestionIndex(q.position() == null ? mapped.size() + 1 : q.position());
            entity.setQuestionType(q.questionType());
            entity.setQuestionText(q.questionText());
            entity.setTrainerHint(q.trainerHint());
            entity.setOptionsRaw(joinValues(q.options()));
            entity.setCorrectAnswersRaw(joinValues(q.correctAnswers()));
            entity.setFullPoints(q.fullPoints() == null ? lesson.getFullPoints() : q.fullPoints());
            entity.setPartialPoints(q.partialPoints() == null ? lesson.getPartialPoints() : q.partialPoints());
            mapped.add(entity);
        }
        lesson.getQuestions().clear();
        lesson.getQuestions().addAll(mapped);
    }

    private boolean isTheoryLesson(Lesson lesson) {
        return lesson.getLessonType() == LessonType.THEORY_TEXT
                || lesson.getLessonType() == LessonType.THEORY_VIDEO
                || lesson.getLessonType() == LessonType.THEORY_PDF;
    }

    private CourseSummaryDto toCourseSummaryDto(Course course) {
        long theoryCount = course.getLessons().stream().filter(this::isTheoryLesson).count();
        long practiceCount = course.getLessons().size() - theoryCount;
        return new CourseSummaryDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverFilePath(),
                course.getSection() == null ? null : course.getSection().getId(),
                course.getSection() == null ? null : course.getSection().getTitle(),
                course.getSection() == null ? null : course.getSection().getPriority(),
                theoryCount,
                practiceCount
        );
    }

    private void validatePracticeRequest(CreatePracticeLessonRequest request) {
        if (request.lessonType().getSubType() != LessonType.LessonSubType.PRACTICE) {
            throw new BadRequestException("lessonType must be one of practice types");
        }

        if (CollectionUtils.isEmpty(request.questions())) {
            throw new BadRequestException("lesson should have 1 question");
        }

        boolean allWithoutPosition = request.questions().stream().allMatch(q -> q.position() == null);
        boolean allWithPosition = request.questions().stream().allMatch(q -> q.position() != null);
        if (!allWithoutPosition && !allWithPosition) {
            throw new BadRequestException("Question positions must be defined for all questions or for no one");
        }

        if (allWithPosition && !request.questions().stream().map(PracticeQuestionRequest::position).collect(Collectors.toSet())
                .equals(IntStream.range(1, request.questions().size() + 1).boxed().collect(Collectors.toSet()))) {
            throw new BadRequestException("Questions positions must be without skipping numbers");
        }

        for (PracticeQuestionRequest q : request.questions()) {
            if (q.partialPoints() != null && q.fullPoints() != null && q.partialPoints() > q.fullPoints()) {
                throw new BadRequestException("partialPoints must be less or equals than fullPoints");
            }
            if (q.questionType() == null) {
                throw new BadRequestException("questionType is required for each question");
            }
            if (StringUtils.isEmpty(q.questionText())) {
                throw new BadRequestException("questionText is required for each question");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && StringUtils.isNoneBlank(q.trainerHint())) {
                throw new BadRequestException("Tests mustn't contain trainerHint");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && CollectionUtils.isEmpty(q.options())) {
                throw new BadRequestException("options are required for test question");
            }
            if (QuestionType.TEST_QUESTIONS.contains(q.questionType()) && CollectionUtils.isEmpty(q.correctAnswers())) {
                throw new BadRequestException("correctAnswers are required for test question");
            }
            if (q.questionType() == QuestionType.SINGLE_CHOICE && q.correctAnswers().size() != 1) {
                throw new BadRequestException("SINGLE_CHOICE must contain exactly one correct answer");
            }
            if (q.questionType() == QuestionType.SINGLE_CHOICE && q.partialPoints() != null) {
                throw new BadRequestException("SINGLE_CHOICE mustn't contain partialPoints");
            }
            if (q.questionType() == QuestionType.OPEN_ANSWER && !CollectionUtils.isEmpty(q.correctAnswers())) {
                throw new BadRequestException("OPEN_ANSWER mustn't contain correctAnswers");
            }
            if (q.position() != null && q.position() < 1) {
                throw new BadRequestException("question position must be >= 1");
            }
        }

    }

    public List<String> splitRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(";;", -1)).toList();
    }

    private void validateStudentsExist(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            throw new BadRequestException("ids must not be empty");
        }
        Set<Long> distinctIds = new HashSet<>(userIds);
        List<AppUser> users = userRepository.findAllById(distinctIds);
        if (users.size() != distinctIds.size()) {
            throw new NotFoundException("Some users were not found");
        }
        boolean hasNonStudent = users.stream().anyMatch(user -> user.getRole() != Role.STUDENT);
        if (hasNonStudent) {
            throw new BadRequestException("Only STUDENT users are allowed for enrollment lists operations");
        }
    }

    private String resolveCurrentActor() {
        try {
            return securityUtils.currentUser().getUsername();
        } catch (Exception ex) {
            return "system";
        }
    }
}
