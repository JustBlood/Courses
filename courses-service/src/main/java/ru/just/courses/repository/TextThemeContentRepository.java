package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.theme.content.TextThemeContent;
import ru.just.courses.repository.projection.TextThemeContentProjection;

import java.util.Optional;

public interface TextThemeContentRepository extends JpaRepository<TextThemeContent, Long> {
    Optional<TextThemeContentProjection> findTextThemeContentByThemeId(Long themeId);
}
