package ru.just.courses.service.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.controller.exception.BadArgumentsException;
import ru.just.courses.dto.exercise.MultiTestExerciseDto;
import ru.just.courses.dto.exercise.PatchMultiTestExerciseDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.exercise.MultiTestExercise;
import ru.just.courses.repository.ExerciseRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.courses.service.ThemeService;

@Service
@RequiredArgsConstructor
public class MultiTestExerciseService {
    private final ExerciseRepository<MultiTestExercise> multiTestExerciseRepository;
    private final ExerciseService exerciseService;
    private final ThemeService themeService;

    @Transactional
    public void createMultiTestExercise(Long themeId, MultiTestExerciseDto testExerciseDto) {
        if (exerciseService.isExerciseWithOrdinalNumberExists(themeId, testExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        final MultiTestExercise entity = testExerciseDto.toEntity();
        entity.setTheme(new Theme().withId(themeId));
        multiTestExerciseRepository.save(entity);
    }

    @Transactional
    public MultiTestExerciseDto updateMultiTestExercise(Long themeId, Integer ordinalNumber, PatchMultiTestExerciseDto patchTestExerciseDto) {
        if (!ordinalNumber.equals(patchTestExerciseDto.getOrdinalNumber())
                && exerciseService.isExerciseWithOrdinalNumberExists(themeId, patchTestExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        themeService.checkCourseAuthor(themeId);

        MultiTestExercise exercise = multiTestExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        if (patchTestExerciseDto.getCorrectAnswers() != null) exercise.setCorrectAnswers(patchTestExerciseDto.getCorrectAnswers());
        if (patchTestExerciseDto.getPossibleAnswers() != null) exercise.setPossibleAnswers(patchTestExerciseDto.getPossibleAnswers());
        if (patchTestExerciseDto.getOrdinalNumber() != null) exercise.setOrdinalNumber(patchTestExerciseDto.getOrdinalNumber());
        return new MultiTestExerciseDto().fromEntity(exercise);
    }

    public MultiTestExerciseDto getMultiTestExerciseByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber) {
        var exercise = multiTestExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        return new MultiTestExerciseDto().fromEntity(exercise);
    }
}
