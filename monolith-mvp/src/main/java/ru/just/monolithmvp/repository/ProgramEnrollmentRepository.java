package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.ProgramEnrollment;

import java.util.List;

public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long> {
    boolean existsByUserIdAndProgramId(Long userId, Long programId);
    List<ProgramEnrollment> findByUserId(Long userId);
    List<ProgramEnrollment> findByUserIdIn(List<Long> userIds);
    void deleteByUserIdAndProgramId(Long userId, Long programId);
    void deleteByUserId(Long userId);
}