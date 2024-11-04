package ru.just.courses.service.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.model.theme.exercise.Exercise;
import ru.just.courses.repository.ExerciseRepository;
import ru.just.courses.service.ThemeService;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    private final ExerciseRepository<Exercise> exerciseRepository;
    private final ThemeService themeService;

    @Transactional
    public void deleteExerciseFromTheme(Long themeId, Integer ordinalNumber) {
        themeService.checkCourseAuthor(themeId);

        exerciseRepository.deleteByThemeIdAndOrdinalNumber(themeId, ordinalNumber);
    }

    public boolean isExerciseWithOrdinalNumberExists(Long themeId, Integer ordinalNumber) {
        return exerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber).isPresent();
    }
}
