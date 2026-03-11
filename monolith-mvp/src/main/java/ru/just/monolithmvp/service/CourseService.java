package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.course.*;
import ru.just.monolithmvp.dto.lesson.*;
import ru.just.monolithmvp.dto.section.SectionWithCoursesDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.mapper.LessonMapper;
import ru.just.monolithmvp.model.*;
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

    private final CourseLearnerReadService courseLearnerReadService;
    private final CourseAssignmentService courseAssignmentService;

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
        return new CourseAdminDetailsDto(toCourseDtoWithPublicCover(course), lessons);
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
        return toCourseDtoWithPublicCover(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseLearnerReadService.getCourseEntity(courseId);
        submissionRepository.deleteByLesson_Course_Id(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseAssignmentService.deleteByCourseId(courseId);
        fileStorageService.deleteIfExists(course.getCoverFilePath());
        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getMyCourses() {
        Long userId = securityUtils.currentUserId();
        return enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .map(this::toCourseDtoWithPublicCover)
                .toList();
    }

    @Transactional
    public void resetStudentCourseProgress(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ru.just.monolithmvp.exception.NotFoundException("Enrollment not found for user/course"));

        submissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
        enrollment.setStartedAt(null);
        enrollment.setCompletedAt(null);
        enrollmentRepository.save(enrollment);
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
        course.setBlockAfterDeadline(Boolean.TRUE.equals(request.blockAfterDeadline()));
        course.setIncludeInOverallStats(request.includeInOverallStats() == null || request.includeInOverallStats());
        course.setSection(request.sectionId() == null ? sectionService.getDefaultSection() : sectionService.getSectionEntity(request.sectionId()));
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
                dto.blockAfterDeadline(),
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
}
