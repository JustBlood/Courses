package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.GroupCourseAssignment;

import java.util.List;
import java.util.UUID;

public interface GroupCourseAssignmentRepository extends JpaRepository<GroupCourseAssignment, Long> {
    boolean existsByGroupIdAndCourseId(UUID groupId, Long courseId);
    @EntityGraph(attributePaths = {"course", "group"})
    List<GroupCourseAssignment> findByGroupId(UUID groupId);
    void deleteByGroupIdAndCourseId(UUID groupId, Long courseId);
}
