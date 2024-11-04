package ru.just.courses.controller.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.exercise.MultiTestExerciseDto;
import ru.just.courses.dto.exercise.PatchMultiTestExerciseDto;
import ru.just.courses.service.exercise.MultiTestExerciseService;
import ru.just.dtolib.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/theme/{themeId}/exercises/test/multi")
@RequiredArgsConstructor
public class MultiTestExerciseController {
    private final MultiTestExerciseService exerciseService;

    @PostMapping
    public ResponseEntity<ApiResponse> createMultiTestExercise(@PathVariable("themeId") Long themeId,
                                                          @RequestBody MultiTestExerciseDto testExerciseDto) {
        final String message = String.format("Success adding test exercise to theme with id = %s", themeId);
        exerciseService.createMultiTestExercise(themeId, testExerciseDto);
        return new ResponseEntity<>(new ApiResponse(message), HttpStatus.CREATED);
    }

    @GetMapping(value = "/{ordinalNumber}")
    public ResponseEntity<MultiTestExerciseDto> getMultiTestExerciseByOrdinalNumber(@PathVariable("themeId") Long themeId,
                                                                               @PathVariable("ordinalNumber") Integer ordinalNumber) {
        var exerciseDto = exerciseService.getMultiTestExerciseByThemeIdAndOrdinalNumber(themeId, ordinalNumber);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }

    @PatchMapping(value = "/{ordinalNumber}")
    public ResponseEntity<MultiTestExerciseDto> updateMultiTestExercise(@PathVariable("themeId") Long themeId,
                                                              @PathVariable("ordinalNumber") Integer ordinalNumber,
                                                              @RequestBody PatchMultiTestExerciseDto patchTestExerciseDto) {
        var exerciseDto = exerciseService.updateMultiTestExercise(themeId, ordinalNumber, patchTestExerciseDto);
        return new ResponseEntity<>(exerciseDto, HttpStatus.OK);
    }
}
