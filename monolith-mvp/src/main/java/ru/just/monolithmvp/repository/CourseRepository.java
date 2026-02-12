package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
