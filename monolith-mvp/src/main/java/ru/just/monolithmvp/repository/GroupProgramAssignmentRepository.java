package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.GroupProgramAssignment;

import java.util.List;
import java.util.UUID;

public interface GroupProgramAssignmentRepository extends JpaRepository<GroupProgramAssignment, Long> {
    boolean existsByGroupIdAndProgramId(UUID groupId, Long programId);
    List<GroupProgramAssignment> findByGroupId(UUID groupId);
    void deleteByGroupIdAndProgramId(UUID groupId, Long programId);
}
