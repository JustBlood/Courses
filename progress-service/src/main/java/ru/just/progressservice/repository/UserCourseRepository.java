package ru.just.progressservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.progressservice.model.user.UserCourse;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
}
