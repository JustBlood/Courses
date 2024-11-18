package ru.just.progressservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.progressservice.model.user.UserModuleProgress;

import java.util.List;
import java.util.Optional;

public interface UserModuleProgressRepository extends JpaRepository<UserModuleProgress, Long> {
    List<UserModuleProgress> findByCourseProgressId(Long courseProgressId);

    Optional<UserModuleProgress> findByModuleIdAndCourseProgressId(Long moduleId, Long courseProgressId);
}

