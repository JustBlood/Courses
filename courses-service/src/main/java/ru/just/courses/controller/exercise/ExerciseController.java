package ru.just.courses.controller.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.just.courses.service.exercise.ExerciseService;
import ru.just.dtolib.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/theme/{themeId}/exercises")
@RequiredArgsConstructor
public class ExerciseController {
    private final ExerciseService exerciseService;

    @DeleteMapping(value = "/{ordinalNumber}")
    public ResponseEntity<ApiResponse> deleteExercise(@PathVariable("themeId") Long themeId,
                                                      @PathVariable("ordinalNumber") Integer ordinalNumber) {
        final String message = String.format("Success deleting exercise from theme with id = %s", themeId);
        exerciseService.deleteExerciseFromTheme(themeId, ordinalNumber);
        return new ResponseEntity<>(new ApiResponse(message), HttpStatus.OK);
    }
}
