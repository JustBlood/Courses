package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.ProgramCourse;

import java.util.List;

public interface ProgramCourseRepository extends JpaRepository<ProgramCourse, Long> {
    List<ProgramCourse> findByProgramIdOrderByOrderIndexAsc(Long programId);

    void deleteByProgramId(Long programId);
}
