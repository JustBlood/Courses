package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.dto.CourseDto;
import ru.just.courses.dto.CreateCourseDto;
import ru.just.courses.model.course.Course;
import ru.just.courses.model.user.User;
import ru.just.courses.repository.CourseRepository;
import ru.just.courses.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseDto saveCourse(CreateCourseDto courseDto) {
        if (!userRepository.existsById(courseDto.getAuthorId())) {
            throw new NoSuchElementException("user with specified authorId doesn't exists");
        }
        final Course course = courseRepository.save(courseDto.toEntity());
        return new CourseDto().fromEntity(course);
    }

    public void updateCourse(Long id, CreateCourseDto courseDto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("course with specified id doesn't exists"));
        courseRepository.save(courseDto.toEntity().withId(course.getId()));
    }

    public void deleteCourseById(Long id) {
        courseRepository.deleteById(id);
    }

    public List<CourseDto> getCourses(String prefix) {
        return courseRepository.findByTitleLike(prefix + "%").stream()
                .map(new CourseDto()::fromEntity)
                .collect(Collectors.toList());
    }

    public CourseDto getCourseById(Long id) {
        CourseDto dto = new CourseDto();
        dto.fromEntity(courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("course with specified id doesn't exists")));
        return dto;
    }

    public void assignUser(Long courseId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user with specified id doesn't exists"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("course with specified id doesn't exists"));
        user.addCourse(course);
        userRepository.save(user);
    }
}
