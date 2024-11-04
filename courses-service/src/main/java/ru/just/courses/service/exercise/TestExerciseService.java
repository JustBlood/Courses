package ru.just.courses.service.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.controller.exception.BadArgumentsException;
import ru.just.courses.dto.exercise.PatchTestExerciseDto;
import ru.just.courses.dto.exercise.TestExerciseDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.exercise.TestExercise;
import ru.just.courses.repository.ExerciseRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.courses.service.ThemeService;

@Service
@RequiredArgsConstructor
public class TestExerciseService {
    private final ExerciseRepository<TestExercise> testExerciseExerciseRepository;
    private final ThemeService themeService;
    private final ExerciseService exerciseService;

    @Transactional
    public void createTestExercise(Long themeId, TestExerciseDto testExerciseDto) {
        if (exerciseService.isExerciseWithOrdinalNumberExists(themeId, testExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        final TestExercise entity = testExerciseDto.toEntity();
        entity.setTheme(new Theme().withId(themeId));
        testExerciseExerciseRepository.save(entity);
    }

    @Transactional
    public TestExerciseDto updateTestExercise(Long themeId, Integer ordinalNumber, PatchTestExerciseDto patchTestExerciseDto) {
        if (!ordinalNumber.equals(patchTestExerciseDto.getOrdinalNumber())
                && exerciseService.isExerciseWithOrdinalNumberExists(themeId, patchTestExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        themeService.checkCourseAuthor(themeId);

        TestExercise exercise = testExerciseExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        if (patchTestExerciseDto.getAnswer() != null) exercise.setAnswer(patchTestExerciseDto.getAnswer());
        if (patchTestExerciseDto.getPossibleAnswers() != null) exercise.setPossibleAnswers(patchTestExerciseDto.getPossibleAnswers());
        if (patchTestExerciseDto.getOrdinalNumber() != null) exercise.setOrdinalNumber(patchTestExerciseDto.getOrdinalNumber());
        return new TestExerciseDto().fromEntity(exercise);
    }

    public TestExerciseDto getTestExerciseByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber) {
        var exercise = testExerciseExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        return new TestExerciseDto().fromEntity(exercise);
    }
}
