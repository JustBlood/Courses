package ru.just.courses.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.PatchTestExerciseDto;
import ru.just.courses.dto.TestExerciseDto;
import ru.just.courses.service.ExerciseService;
import ru.just.dtolib.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/theme/{themeId}/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ExerciseService exerciseService;

    @PostMapping(value = "/test")
    public ResponseEntity<ApiResponse> createTestExercise(@PathVariable("themeId") Long themeId,
                                                          @RequestBody TestExerciseDto testExerciseDto) {
        final String message = String.format("Success adding test exercise to theme with id = %s", themeId);
        exerciseService.createTestExercise(themeId, testExerciseDto);
        return new ResponseEntity<>(new ApiResponse(message), HttpStatus.CREATED);
    }

    @GetMapping(value = "/test/{ordinalNumber}")
    public ResponseEntity<TestExerciseDto> getTestExerciseByOrdinalNumber(@PathVariable("themeId") Long themeId,
                                                                          @PathVariable("ordinalNumber") Integer ordinalNumber) {
        var exerciseDto = exerciseService.getTestExerciseByThemeIdAndOrdinalNumber(themeId, ordinalNumber);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }

    @PatchMapping(value = "/{ordinalNumber}/test")
    public ResponseEntity<TestExerciseDto> updateTestExercise(@PathVariable("themeId") Long themeId,
                                                              @PathVariable("ordinalNumber") Integer ordinalNumber,
                                                              @RequestBody PatchTestExerciseDto patchTestExerciseDto) {
        var exerciseDto = exerciseService.updateTestExercise(themeId, ordinalNumber, patchTestExerciseDto);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{ordinalNumber}")
    public ResponseEntity<ApiResponse> deleteExercise(@PathVariable("themeId") Long themeId,
                                                      @PathVariable("ordinalNumber") Integer ordinalNumber) {
        final String message = String.format("Success deleting exercise from theme with id = %s", themeId);
        exerciseService.deleteExerciseFromTheme(themeId, ordinalNumber);
        return new ResponseEntity<>(new ApiResponse(message), HttpStatus.OK);
    }
}
