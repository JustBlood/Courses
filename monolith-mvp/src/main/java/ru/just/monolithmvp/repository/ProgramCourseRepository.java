package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import ru.just.monolithmvp.model.ProgramCourse;

import java.util.Collection;
import java.util.List;

public interface ProgramCourseRepository extends JpaRepository<ProgramCourse, Long> {
    @EntityGraph(attributePaths = "course")
    List<ProgramCourse> findByProgramIdOrderByOrderIndexAsc(Long programId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByProgramId(Long programId);
}
