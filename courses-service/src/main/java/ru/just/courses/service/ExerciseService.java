package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.dto.PatchTestExerciseDto;
import ru.just.courses.dto.TestExerciseDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.exercise.Exercise;
import ru.just.courses.model.theme.exercise.TestExercise;
import ru.just.courses.repository.ExerciseRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    private final ExerciseRepository<Exercise> exerciseRepository;
    private final ExerciseRepository<TestExercise> testExerciseExerciseRepository;
    private final ThemeService themeService;

    public void createTestExercise(Long themeId, TestExerciseDto testExerciseDto) {
        final TestExercise entity = testExerciseDto.toEntity();
        entity.setTheme(new Theme().withId(themeId));
        exerciseRepository.save(entity);
    }

    @Transactional
    public TestExerciseDto updateTestExercise(Long themeId, Integer ordinalNumber, PatchTestExerciseDto patchTestExerciseDto) {
        TestExercise exercise = testExerciseExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        exercise.setAnswer(patchTestExerciseDto.getAnswer());
        exercise.setPossibleAnswers(patchTestExerciseDto.getPossibleAnswers());
        exercise.setOrdinalNumber(patchTestExerciseDto.getOrdinalNumber());
        return new TestExerciseDto().fromEntity(exercise);
    }

    @Transactional
    public void deleteExerciseFromTheme(Long themeId, Integer ordinalNumber) {
        themeService.checkCourseAuthor(themeId);

        exerciseRepository.deleteByThemeIdAndOrdinalNumber(themeId, ordinalNumber);
    }

    public TestExerciseDto getTestExerciseByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber) {
        var exercise = testExerciseExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        return new TestExerciseDto().fromEntity(exercise);
    }
}
