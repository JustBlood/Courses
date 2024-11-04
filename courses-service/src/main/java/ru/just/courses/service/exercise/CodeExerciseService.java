package ru.just.courses.service.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.controller.exception.BadArgumentsException;
import ru.just.courses.dto.exercise.CodeExerciseDto;
import ru.just.courses.dto.exercise.PatchCodeExerciseDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.exercise.CodeExercise;
import ru.just.courses.repository.ExerciseRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.courses.service.ThemeService;

@Service
@RequiredArgsConstructor
public class CodeExerciseService {
    private final ExerciseRepository<CodeExercise> codeExerciseRepository;
    private final ThemeService themeService;
    private final ExerciseService exerciseService;

    @Transactional
    public void createCodeExercise(Long themeId, CodeExerciseDto codeExerciseDto) {
        if (exerciseService.isExerciseWithOrdinalNumberExists(themeId, codeExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        final CodeExercise entity = codeExerciseDto.toEntity();
        entity.setTheme(new Theme().withId(themeId));
        codeExerciseRepository.save(entity);
    }

    @Transactional
    public CodeExerciseDto updateCodeExercise(Long themeId, Integer ordinalNumber, PatchCodeExerciseDto patchCodeExerciseDto) {
        if (!ordinalNumber.equals(patchCodeExerciseDto.getOrdinalNumber())
        && exerciseService.isExerciseWithOrdinalNumberExists(themeId, patchCodeExerciseDto.getOrdinalNumber())) {
            throw new BadArgumentsException("exercise with specified ordinal number already exists in theme");
        }
        themeService.checkCourseAuthor(themeId);

        CodeExercise exercise = codeExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        if (patchCodeExerciseDto.getCodeTests() != null) exercise.setCodeTests(patchCodeExerciseDto.getCodeTests());
        if (patchCodeExerciseDto.getOrdinalNumber() != null) exercise.setOrdinalNumber(patchCodeExerciseDto.getOrdinalNumber());
        return new CodeExerciseDto().fromEntity(exercise);
    }

    public CodeExerciseDto getCodeExerciseByThemeIdAndOrdinalNumber(Long themeId, Integer ordinalNumber) {
        var exercise = codeExerciseRepository.findByThemeIdAndOrdinalNumber(themeId, ordinalNumber)
                .orElseThrow(() -> new EntityNotFoundException("exercise not found"));
        return new CodeExerciseDto().fromEntity(exercise);
    }
}
