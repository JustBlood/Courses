package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.Course;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySectionId(Long sectionId);

    @Query("""
        select c from Course c join fetch c.section
        where not exists (
            select 1
            from GroupCourseAssignment gca
            where gca.group.id = :groupId and gca.course.id = c.id
        )
        """)
    List<Course> findAllNotAssignedToGroup(UUID groupId);
}
