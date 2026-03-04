package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.LearningProgram;

public interface LearningProgramRepository extends JpaRepository<LearningProgram, Long> {
}
