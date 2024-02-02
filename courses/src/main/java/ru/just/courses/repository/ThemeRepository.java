package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.theme.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
}
