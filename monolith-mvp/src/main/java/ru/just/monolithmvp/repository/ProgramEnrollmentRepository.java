package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.ProgramEnrollment;

import java.util.List;
import java.util.Optional;

public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long> {
    boolean existsByUserIdAndProgramId(Long userId, Long programId);

    @Query("""
            select pe
            from ProgramEnrollment pe
            join fetch pe.user
            join fetch pe.program
            where pe.user.id = :userId
            """)
    List<ProgramEnrollment> findByUserId(Long userId);

    @Query("""
            select pe
            from ProgramEnrollment pe
            join fetch pe.user
            join fetch pe.program
            where pe.program.id = :programId
            """)
    List<ProgramEnrollment> findByProgramId(Long programId);

    Optional<ProgramEnrollment> findByUserIdAndProgramId(Long userId, Long programId);

    void deleteByProgramId(Long programId);
}
