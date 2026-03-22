package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.LearningProgram;

import java.util.List;
import java.util.UUID;

public interface LearningProgramRepository extends JpaRepository<LearningProgram, Long> {
    @Query("""
        select c from LearningProgram c
        where not exists (
            select 1
            from GroupProgramAssignment gpa
            where gpa.group.id = :groupId and gpa.program.id = c.id
        )
        """)
    List<LearningProgram> findAllNotAssignedToGroup(UUID groupId);
}
