package ru.just.progressservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.progressservice.model.user.UserThemeProgress;

import java.util.List;
import java.util.Optional;

public interface UserThemeProgressRepository extends JpaRepository<UserThemeProgress, Long> {
    List<UserThemeProgress> findByModuleProgressId(Long moduleProgressId);

    Optional<UserThemeProgress> findByThemeIdAndModuleProgressId(Long themeId, Long moduleProgressId);
}

