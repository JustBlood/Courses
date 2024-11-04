package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.theme.exercise.Exercise;

import java.util.Optional;

public interface ExerciseRepository<T extends Exercise> extends JpaRepository<T, Long> {
    void deleteByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber);

    Optional<T> findByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber);
}
