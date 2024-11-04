package ru.just.courses.controller.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.exercise.CodeExerciseDto;
import ru.just.courses.dto.exercise.PatchCodeExerciseDto;
import ru.just.courses.service.exercise.CodeExerciseService;
import ru.just.dtolib.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/theme/{themeId}/exercises/code")
@RequiredArgsConstructor
public class CodeExerciseController {
    private final CodeExerciseService exerciseService;

    @PostMapping
    public ResponseEntity<ApiResponse> createCodeExercise(@PathVariable("themeId") Long themeId,
                                                          @RequestBody CodeExerciseDto testExerciseDto) {
        final String message = String.format("Success adding test exercise to theme with id = %s", themeId);
        exerciseService.createCodeExercise(themeId, testExerciseDto);
        return new ResponseEntity<>(new ApiResponse(message), HttpStatus.CREATED);
    }

    @GetMapping(value = "/{ordinalNumber}")
    public ResponseEntity<CodeExerciseDto> getCodeExerciseByOrdinalNumber(@PathVariable("themeId") Long themeId,
                                                                          @PathVariable("ordinalNumber") Integer ordinalNumber) {
        var exerciseDto = exerciseService.getCodeExerciseByThemeIdAndOrdinalNumber(themeId, ordinalNumber);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }

    @PatchMapping(value = "/{ordinalNumber}")
    public ResponseEntity<CodeExerciseDto> updateCodeExercise(@PathVariable("themeId") Long themeId,
                                                              @PathVariable("ordinalNumber") Integer ordinalNumber,
                                                              @RequestBody PatchCodeExerciseDto patchCodeExerciseDto) {
        var exerciseDto = exerciseService.updateCodeExercise(themeId, ordinalNumber, patchCodeExerciseDto);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }
}
