package ru.just.progressservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.progressservice.model.user.UserCourse;
import ru.just.progressservice.repository.UserCourseRepository;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final UserCourseRepository userCourseRepository;

    public void assignUser(Long courseId, Long userId) {
        userCourseRepository.save(new UserCourse(courseId, userId));
    }
}
