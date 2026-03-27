package ru.just.monolithmvp.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.dto.section.SectionWithCoursesDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.LessonMapper;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;
    private final SecurityUtils securityUtils;
    private final SectionService sectionService;
    private final FileStorageService fileStorageService;

    private final CourseLessonAdminService lessonAdminService;
    private final CourseLearnerReadService courseLearnerReadService;
    private final CourseAssignmentService courseAssignmentService;
    private final CourseEnrollmentLifecycleService courseEnrollmentLifecycleService;

    @Transactional
    public CourseDto createCourse(CreateCourseRequest request) {
        Course course = new Course();
        applyCourseFields(course, request);
        course.setCreatedByAdminId(securityUtils.currentUserId());
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<SectionWithCoursesDto> getCourseSummariesBySection() {
        return toSectionWithCoursesDto(courseRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> getReviewerCourseSummaries(Long adminId) {
        return courseAssignmentService.findCoursesThatAdminCanReview(adminId).stream()
                .map(this::toCourseSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> getMyReviewerCourseSummaries() {
        return getReviewerCourseSummaries(securityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public CourseAdminDetailsDto getCourseAdminDetails(Long courseId) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        List<LessonDto> lessons = lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(lessonMapper::toDto)
                .toList();
        return new CourseAdminDetailsDto(courseMapper.toDto(course), lessons);
    }

    @Transactional
    public CourseDto updateCourse(Long courseId, CreateCourseRequest request) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        String oldCoverPath = course.getCoverFilePath();
        setLessonsToNewPositionsIfNeeded(courseId, request);
        applyCourseFields(course, request);
        if (!Objects.equals(oldCoverPath, course.getCoverFilePath())) {
            fileStorageService.deleteIfExists(oldCoverPath);
        }
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        submissionRepository.deleteByLesson_Course_Id(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseAssignmentService.deleteCourseReviewersByCourseId(courseId);
        fileStorageService.deleteIfExists(course.getCoverFilePath());
        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public List<CourseLearnerDto> getMyCourses() {
        Long userId = securityUtils.currentUserId();

        return courseLearnerReadService.getCoursesForLearner(userId);
    }

    @Transactional
    public void resetStudentCourseProgress(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ru.just.monolithmvp.exception.NotFoundException("Enrollment not found for user/course");
        }
        courseEnrollmentLifecycleService.resetCourseProgress(userId, courseId);
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
        lessons.forEach(lesson -> lesson.setPosition(lesson.getPosition() + offset));
        lessonRepository.saveAllAndFlush(lessons);
        lessons.forEach(lesson -> lesson.setPosition(request.lessonIdToPosition().get(lesson.getId())));
        lessonRepository.saveAll(lessons);
    }

    private void applyCourseFields(Course course, CreateCourseRequest request) {
        course.setTitle(patchValue(request.title(), course.getTitle()));
        course.setDescription(patchValue(request.description(), course.getDescription()));
        course.setAuthorFullName(patchValue(request.authorFullName(), course.getAuthorFullName()));
        if (request.coverFilePath() != null) {
            final String newCoverFilePath = fileStorageService.normalizeStoredPath(request.coverFilePath());
            if (newCoverFilePath != null && !fileStorageService.isFileExistsByRelativePath(newCoverFilePath)) {
                throw new BadRequestException("New avatar path is not valid or file does not exists.");
            }
            course.setCoverFilePath(newCoverFilePath);
        }
        course.setDeadlineDays(request.deadlineDays());
        course.setLessonsFreeOrder(request.lessonsFreeOrder());
        course.setSection(request.sectionId() != null
                ? sectionService.getSectionEntity(request.sectionId())
                : course.getSection() != null
                    ? course.getSection()
                    : sectionService.getDefaultSection());
    }

    private <T> T patchValue(T requestedValue, T currentValue) {
        return requestedValue != null ? requestedValue : currentValue;
    }

    private CourseSummaryDto toCourseSummaryDto(Course course) {
        long theoryCount = course.getLessons().stream().filter(lesson -> lesson.getLessonType().isTheory()).count();
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

    public void resetStudentLessonProgress(@NotNull Long userId, @NotNull Long courseId, @NotNull Long lessonId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ru.just.monolithmvp.exception.NotFoundException("Enrollment not found for user/course");
        }

        lessonAdminService.resetLessonProgress(userId, courseId, lessonId);
    }

    @Transactional
    public void resetLessonProgressForAll(@NotNull Long courseId, @NotNull Long lessonId) {
        lessonAdminService.resetLessonProgressForAll(courseId, lessonId);
    }
}
