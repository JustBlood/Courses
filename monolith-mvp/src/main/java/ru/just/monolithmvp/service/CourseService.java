package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.dto.section.SectionWithCoursesDto;
import ru.just.monolithmvp.dto.user.UserDto;
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
    private final FileStorageService fileStorageService;

    @Transactional
    public CourseDto createCourse(CreateCourseRequest request) {
        Course course = new Course();
        applyCourseFields(course, request);
        course.setCreatedByAdminId(securityUtils.currentUserId());
        return toCourseDtoWithPublicCover(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<SectionWithCoursesDto> getCourseSummariesBySection() {
        return toSectionWithCoursesDto(courseRepository.findAll());
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
        return new CourseAdminDetailsDto(toCourseDtoWithPublicCover(course), lessons);
    }

    @Transactional
    public CourseDto updateCourse(Long courseId, CreateCourseRequest request) {
        Course course = getCourseEntity(courseId);
        String oldCoverPath = course.getCoverFilePath();
        setLessonsToNewPositionsIfNeeded(courseId, request);
        applyCourseFields(course, request);
        if (!Objects.equals(oldCoverPath, course.getCoverFilePath())) {
            fileStorageService.deleteIfExists(oldCoverPath);
        }
        return toCourseDtoWithPublicCover(courseRepository.save(course));
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
        lessons.forEach(lesson -> lesson.setPosition(request.lessonIdToPosition().get(lesson.getId())));
        lessonRepository.saveAll(lessons);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = getCourseEntity(courseId);
        submissionRepository.deleteByLesson_Course_Id(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseReviewerRepository.deleteByCourseId(courseId);
        fileStorageService.deleteIfExists(course.getCoverFilePath());
        courseRepository.delete(course);
    }

    @Transactional
    public LessonDto createTheoryLesson(Long courseId, CreateTheoryLessonRequest request) {
        Course course = getCourseEntity(courseId);
        final int position = resolveCreateLessonPosition(courseId, request.position());
        validateCreateTheoryRequest(request);

        TheoryLesson lesson = new TheoryLesson();
        lesson.setCourse(course);
        lesson.setPosition(position);
        applyTheoryLessonFields(lesson, request);

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto createPracticeLesson(Long courseId, CreatePracticeLessonRequest request) {
        Course course = getCourseEntity(courseId);
        validateCreatePracticeRequest(request);
        final Integer lessonPosition = resolveCreateLessonPosition(courseId, request.position());

        PracticeLesson lesson = new PracticeLesson();
        lesson.setCourse(course);
        lesson.setPosition(lessonPosition);
        applyPracticeLessonFields(lesson, request);

        applyQuestionPool(lesson, request.questions(), Boolean.TRUE.equals(lesson.getEvaluateByCorrectCount()));

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
    public LessonDto updateTheoryLesson(Long courseId, Long lessonId, UpdateTheoryLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof TheoryLesson lesson)) {
            throw new BadRequestException("Lesson is not theory");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        applyLessonPositionPatch(lesson, request.position());

        applyTheoryLessonFields(lesson, toCreateTheoryLessonRequest(request));

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonDto updatePracticeLesson(Long courseId, Long lessonId, UpdatePracticeLessonRequest request) {
        Lesson lessonEntity = getLessonEntity(lessonId);
        if (!(lessonEntity instanceof PracticeLesson lesson)) {
            throw new BadRequestException("Lesson is not practice");
        }
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new BadRequestException("Lesson does not belong to course");
        }

        applyLessonPositionPatch(lesson, request.position());

        CreatePracticeLessonRequest patchRequest = toCreatePracticeLessonRequest(request);
        validateUpdatePracticeRequest(patchRequest);
        applyPracticeLessonFields(lesson, patchRequest);

        if (!CollectionUtils.isEmpty(patchRequest.questions())) {
            applyQuestionPool(lesson, patchRequest.questions(), Boolean.TRUE.equals(lesson.getEvaluateByCorrectCount()));
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
        assertCourseDeadlineNotExceededForStudent(userId, courseId);
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
                fileStorageService.normalizeStoredPath(course.getCoverFilePath()),
                course.getDeadlineDays(),
                lessons
        );
    }

    @Transactional
    public void enrollUnenrollStudents(Long courseId, Set<Long> idsToEnroll, Set<Long> idsToUnEnroll) {
        String actor = resolveCurrentActor();

        idsToEnroll.forEach(id -> enrollStudentToCourse(courseId, id, actor));
        idsToUnEnroll.forEach(id -> unenrollStudentFromCourse(courseId, id, actor));
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public List<CourseDto> getMyCourses() {
        Long userId = securityUtils.currentUserId();
        return enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .map(this::toCourseDtoWithPublicCover)
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

    @Transactional(readOnly = true)
    public void assertCourseDeadlineNotExceededForStudent(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BadRequestException("Student is not enrolled in this course"));
        Integer deadlineDays = enrollment.getCourse().getDeadlineDays();
        if (deadlineDays == null || deadlineDays <= 0) {
            return;
        }

        LocalDateTime deadlineAt = enrollment.getEnrolledAt().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BadRequestException("Course deadline exceeded");
        }
    }

    private void validateStudentEnrolled(Long userId, Long courseId) {
        if (!isUserEnrolled(userId, courseId)) { // FIXME: транзакция
            throw new BadRequestException("Student is not enrolled in this course");
        }
    }

    private void applyCourseFields(Course course, CreateCourseRequest request) {
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setAuthorFullName(request.authorFullName());
        final String newCoverFilePath = fileStorageService.normalizeStoredPath(request.coverFilePath());
        if (newCoverFilePath != null && !fileStorageService.isFileExistsByRelativePath(newCoverFilePath)) {
            throw new BadRequestException("New avatar path is not valid or file does not exists.");
        }
        course.setCoverFilePath(newCoverFilePath);
        course.setPassingThresholdPercent(request.passingThresholdPercent() == null ? 100 : request.passingThresholdPercent());
        course.setDeadlineDays(request.deadlineDays());
        course.setLessonsFreeOrder(Boolean.TRUE.equals(request.lessonsFreeOrder()));
        course.setAllowContinueAfterFail(Boolean.TRUE.equals(request.allowContinueAfterFail()));
        course.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
        course.setKeepAccessAfterDeadline(Boolean.TRUE.equals(request.keepAccessAfterDeadline()));
        course.setIncludeInOverallStats(request.includeInOverallStats() == null || request.includeInOverallStats());
        course.setSection(request.sectionId() == null ? sectionService.getDefaultSection() : sectionService.getSectionEntity(request.sectionId()));
    }

    private void applyCommonLessonFields(Lesson lesson,
                                         String title,
                                         String description,
                                         Boolean stopLesson,
                                         Boolean blockedDuringAttempt,
                                         Integer attemptLimit,
                                         Integer timeLimitMinutes) {
        lesson.setTitle(Optional.ofNullable(title).orElse(lesson.getTitle()));
        lesson.setDescription(Optional.ofNullable(description).orElse(lesson.getDescription()));
        lesson.setStopLesson(Optional.ofNullable(stopLesson).orElse(lesson.getStopLesson()));
        lesson.setBlockedDuringAttempt(Optional.ofNullable(blockedDuringAttempt).orElse(lesson.getBlockedDuringAttempt()));
        lesson.setAttemptLimit(Optional.ofNullable(attemptLimit).orElse(lesson.getAttemptLimit()));
        lesson.setTimeLimitMinutes(Optional.ofNullable(timeLimitMinutes).orElse(lesson.getTimeLimitMinutes()));
    }

    private void applyTheoryLessonFields(TheoryLesson lesson, CreateTheoryLessonRequest request) {
        applyCommonLessonFields(lesson, request.title(), request.description(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());
        lesson.setContentType(patchValue(request.contentType(), lesson.getContentType()));
        if (LessonType.THEORY_PDF.equals(lesson.getLessonType())) {
            if (request.content() != null && !fileStorageService.isFileExistsByRelativePath(request.content())) {
                throw new BadRequestException("file is not exists");
            }
            fileStorageService.deleteIfExists(lesson.getContent());
        }
        lesson.setContent(patchValue(request.content(), lesson.getContent()));
        lesson.setFullPoints(patchValue(request.fullPoints(), lesson.getFullPoints()));
        lesson.setPartialPoints(0);
    }

    private void applyPracticeLessonFields(PracticeLesson lesson, CreatePracticeLessonRequest request) {
        applyCommonLessonFields(lesson, request.title(), request.description(), request.stopLesson(),
                request.blockedDuringAttempt(), request.attemptLimit(), request.timeLimitMinutes());
        lesson.setPassingThresholdPercent(patchValue(request.passingThresholdPercent(), lesson.getPassingThresholdPercent()));
        lesson.setEvaluateByCorrectCount(patchValue(request.evaluateByCorrectCount(), lesson.getEvaluateByCorrectCount()));
        lesson.setRandomQuestionCount(patchValue(request.randomQuestionCount(), lesson.getRandomQuestionCount()));
        lesson.setShuffleOnEveryAttempt(patchValue(request.shuffleOptions(), lesson.getShuffleOnEveryAttempt()));
        lesson.setShowQuestionStatus(patchValue(request.showQuestionStatus(), lesson.getShowQuestionStatus()));
        lesson.setShowCorrectAnswersAfterCompletion(patchValue(request.showCorrectAnswers(), lesson.getShowCorrectAnswersAfterCompletion()));
        lesson.setLessonType(patchValue(request.lessonType(), lesson.getLessonType()));

        if (!CollectionUtils.isEmpty(request.questions())) {
            lesson.setFullPoints(request.questions().stream()
                    .mapToInt(q -> resolveQuestionFullPoints(q, Boolean.TRUE.equals(lesson.getEvaluateByCorrectCount())))
                    .sum());
        }
        if (request.partialPoints() != null) {
            lesson.setPartialPoints(request.partialPoints());
        }

        if (lesson.getPartialPoints() > lesson.getFullPoints()) {
            throw new BadRequestException("partialPoints > fullPoints");
        }
    }

    private <T> T patchValue(T requestedValue, T currentValue) {
        return requestedValue != null ? requestedValue : currentValue;
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

    private void applyQuestionPool(PracticeLesson lesson,
                                   List<PracticeQuestionRequest> questions,
                                   boolean evaluateByCorrectCount) {
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
            int resolvedQuestionFullPoints = resolveQuestionFullPoints(q, evaluateByCorrectCount);
            entity.setFullPoints(resolvedQuestionFullPoints);
            int resolvedQuestionPartialPoints = q.partialPoints() == null ? lesson.getPartialPoints() : q.partialPoints();
            entity.setPartialPoints(Math.min(resolvedQuestionPartialPoints, resolvedQuestionFullPoints));
            mapped.add(entity);
        }
        lesson.getQuestions().clear();
        lesson.getQuestions().addAll(mapped);
    }

    private int resolveQuestionFullPoints(PracticeQuestionRequest question, boolean evaluateByCorrectCount) {
        if (evaluateByCorrectCount) {
            return 1;
        }
        return Optional.ofNullable(question.fullPoints()).orElse(1);
    }

    private CreateTheoryLessonRequest toCreateTheoryLessonRequest(UpdateTheoryLessonRequest request) {
        return new CreateTheoryLessonRequest(
                request.position(),
                request.title(),
                request.description(),
                null,
                null,
                null,
                request.stopLesson(),
                request.blockedDuringAttempt(),
                request.attemptLimit(),
                request.timeLimitMinutes(),
                request.contentType(),
                request.content(),
                request.fullPoints(),
                null
        );
    }

    private CreatePracticeLessonRequest toCreatePracticeLessonRequest(UpdatePracticeLessonRequest request) {
        return new CreatePracticeLessonRequest(
                request.position(),
                request.title(),
                request.description(),
                null,
                null,
                null,
                request.stopLesson(),
                request.blockedDuringAttempt(),
                request.attemptLimit(),
                request.timeLimitMinutes(),
                request.lessonType(),
                request.partialPoints(),
                request.passingThresholdPercent(),
                request.evaluateByCorrectCount(),
                request.randomQuestionCount(),
                request.shuffleOptions(),
                request.showQuestionStatus(),
                request.showCorrectAnswers(),
                request.questions()
        );
    }

    private int resolveCreateLessonPosition(Long courseId, Integer requestedPosition) {
        int nextPosition = nextLessonPosition(courseId);
        if (requestedPosition == null) {
            return nextPosition;
        }

        long lessonCount = lessonRepository.countByCourseId(courseId);
        if (requestedPosition < 1 || requestedPosition > lessonCount + 1) {
            throw new BadRequestException("lesson position must be between 1 and " + (lessonCount + 1));
        }

        if (requestedPosition <= lessonCount) {
            List<Lesson> lessonsToShift = lessonRepository
                    .findByCourse_IdAndPositionGreaterThanEqualOrderByPositionDesc(courseId, requestedPosition);
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() + 1));
            lessonRepository.saveAll(lessonsToShift);
        }

        return requestedPosition;
    }

    private void applyLessonPositionPatch(Lesson lesson, Integer requestedPosition) {
        if (requestedPosition == null || requestedPosition.equals(lesson.getPosition())) {
            return;
        }

        long lessonCount = lessonRepository.countByCourseId(lesson.getCourse().getId());
        if (requestedPosition < 1 || requestedPosition > lessonCount) {
            throw new BadRequestException("lesson position must be between 1 and " + lessonCount);
        }

        Integer currentPosition = lesson.getPosition();
        lesson.setPosition(-1);
        lessonRepository.saveAndFlush(lesson);

        List<Lesson> lessonsToShift;
        if (requestedPosition < currentPosition) {
            lessonsToShift = lessonRepository.findByCourse_IdAndPositionBetweenOrderByPositionAsc(
                    lesson.getCourse().getId(),
                    requestedPosition,
                    currentPosition - 1
            );
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() + 1));
        } else {
            lessonsToShift = lessonRepository.findByCourse_IdAndPositionBetweenOrderByPositionAsc(
                    lesson.getCourse().getId(),
                    currentPosition + 1,
                    requestedPosition
            );
            lessonsToShift.forEach(existing -> existing.setPosition(existing.getPosition() - 1));
        }

        lessonRepository.saveAll(lessonsToShift);
        lesson.setPosition(requestedPosition);
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
                fileStorageService.normalizeStoredPath(course.getCoverFilePath()),
                course.getSection().getId(),
                course.getSection().getTitle(),
                course.getSection().getPriority(),
                theoryCount,
                practiceCount
        );
    }

    private CourseDto toCourseDtoWithPublicCover(Course course) {
        CourseDto dto = courseMapper.toDto(course);
        return new CourseDto(
                dto.id(),
                dto.title(),
                dto.description(),
                dto.authorFullName(),
                fileStorageService.normalizeStoredPath(dto.coverFilePath()),
                dto.passingThresholdPercent(),
                dto.deadlineDays(),
                dto.lessonsFreeOrder(),
                dto.allowContinueAfterFail(),
                dto.blockAfterDeadline(),
                dto.keepAccessAfterDeadline(),
                dto.includeInOverallStats(),
                dto.sectionId(),
                dto.sectionTitle(),
                dto.sectionPriority()
        );
    }

    private List<SectionWithCoursesDto> toSectionWithCoursesDto(List<Course> courses) {
        return courses.stream()
                .collect(Collectors.groupingBy(Course::getSection)).entrySet()
                .stream()
                .map(e -> {
                    final List<CourseSummaryDto> coursesBySection = e.getValue().stream().map(this::toCourseSummaryDto).toList();
                    return new SectionWithCoursesDto(e.getKey().getId(), e.getKey().getTitle(), e.getKey().getDescription(), e.getKey().getPriority(), coursesBySection);
                })
                .sorted(Comparator.comparing(SectionWithCoursesDto::priority))
                .toList();
    }

    private void validateUpdatePracticeRequest(CreatePracticeLessonRequest request) {
        if (request.lessonType() != null && request.lessonType().getSubType() != LessonType.LessonSubType.PRACTICE) {
            throw new BadRequestException("lessonType must be one of practice types");
        }

        if (request.questions() == null) {
            return;
        }

        checkPracticeLessonQuestions(request);

    }

    private void validateCreateTheoryRequest(CreateTheoryLessonRequest request) {
        if (StringUtils.isBlank(request.title())) {
            throw new BadRequestException("title is required");
        }
        if (request.contentType() == null) {
            throw new BadRequestException("contentType is required");
        }
        if (StringUtils.isBlank(request.content())) {
            throw new BadRequestException("content is required");
        }
    }

    private void validateCreatePracticeRequest(CreatePracticeLessonRequest request) {
        if (StringUtils.isBlank(request.title())) {
            throw new BadRequestException("title is required");
        }
        if (request.lessonType() == null || request.lessonType().getSubType() != LessonType.LessonSubType.PRACTICE) {
            throw new BadRequestException("lessonType must be one of practice types");
        }

        checkPracticeLessonQuestions(request);

    }

    private void checkPracticeLessonQuestions(CreatePracticeLessonRequest request) {
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
    }

    private String resolveCurrentActor() {
        return securityUtils.resolveCurrentActor();
    }

    @Transactional
    public void assignUnassignReviewers(Long courseId, Set<Long> userIdsToAssign, Set<Long> userIdsToUnassign) {
        userIdsToAssign.forEach(reviewerId -> assignReviewerToCourse(courseId, reviewerId));
        userIdsToUnassign.forEach(reviewerId -> unassignReviewerFromCourse(courseId, reviewerId));

    }

    @Transactional
    public void resetStudentCourseProgress(Long userId, Long courseId) {
        getCourseEntity(courseId);
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found for user/course"));

        submissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
        enrollment.setStartedAt(null);
        enrollment.setCompletedAt(null);
        enrollmentRepository.save(enrollment);
    }
}
