package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.ProgramEnrollment;

import java.util.List;
import java.util.Optional;

public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long> {
    boolean existsByUserIdAndProgramId(Long userId, Long programId);
    List<ProgramEnrollment> findByUserId(Long userId);
    List<ProgramEnrollment> findByProgramId(Long programId);
    Optional<ProgramEnrollment> findByUserIdAndProgramId(Long userId, Long programId);
}
