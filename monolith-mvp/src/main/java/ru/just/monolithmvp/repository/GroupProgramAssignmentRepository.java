package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.GroupProgramAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupProgramAssignmentRepository extends JpaRepository<GroupProgramAssignment, Long> {
    boolean existsByGroupIdAndProgramId(UUID groupId, Long programId);
    @EntityGraph(attributePaths = {"group", "program"})
    List<GroupProgramAssignment> findByGroupId(UUID groupId);
    List<GroupProgramAssignment> findByProgramId(Long programId);
    Optional<GroupProgramAssignment> findByGroupIdAndProgramId(UUID groupId, Long programId);
    void deleteByGroupIdAndProgramId(UUID groupId, Long programId);
    void deleteByProgramId(Long programId);
}
