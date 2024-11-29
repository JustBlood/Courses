package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.controller.exception.MethodNotAllowedException;
import ru.just.courses.dto.CourseDto;
import ru.just.courses.dto.CreateCourseDto;
import ru.just.courses.dto.UpdateCourseDto;
import ru.just.courses.model.course.Course;
import ru.just.courses.repository.CourseRepository;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final ThreadLocalTokenService tokenService;

    public CourseDto saveCourse(CreateCourseDto courseDto) {
        final Course entity = courseDto.toEntity();
        entity.setAuthorId(tokenService.getUserId());
        final Course course = courseRepository.save(entity);
        return new CourseDto().fromEntity(course);
    }

    public void updateCourse(Long courseId, UpdateCourseDto courseDto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException(
                        "course with specified courseId doesn't exists"
                ));
        if (!course.getAuthorId().equals(tokenService.getUserId())) {
            throw new MethodNotAllowedException("You are not a course author");
        }
        course.setDescription(courseDto.getDescription());
        course.setTitle(courseDto.getTitle());
        course.setCompletionTimeInHours(courseDto.getCompletionTimeInHours());
        courseRepository.save(course);
    }

    public void deleteCourseById(Long id) {
        final Optional<Course> course = courseRepository.findById(id);
        if (course.isPresent() && !course.get().getAuthorId().equals(tokenService.getUserId())) {
            throw new MethodNotAllowedException("You are not a course author");
        }
        courseRepository.deleteById(id);
    }

    public List<CourseDto> getCourses(String prefix) {
        return courseRepository.findByTitleLikeIgnoreCase(prefix + "%").stream()
                .map(new CourseDto()::fromEntity)
                .collect(Collectors.toList());
    }

    public CourseDto getCourseById(Long id) {
        CourseDto dto = new CourseDto();
        dto.fromEntity(courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "course with specified id doesn't exists"
                )));
        return dto;
    }
}
