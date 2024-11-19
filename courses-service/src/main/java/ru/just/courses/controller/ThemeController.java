package ru.just.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.CreateThemeDto;
import ru.just.courses.dto.ThemeDto;
import ru.just.courses.service.ThemeService;
import ru.just.dtolib.response.ApiResponse;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/themes")
public class ThemeController {
    private final ThemeService themeService;

    @GetMapping("/byCourse/{courseId}")
    public ResponseEntity<List<ThemeDto>> getThemesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.of(themeService.getThemesByCourseId(courseId));
    }

    @GetMapping("/{themeId}")
    public ResponseEntity<ThemeDto> getThemeById(@PathVariable Long themeId) {
        return ResponseEntity.of(themeService.findThemeById(themeId));
    }

    @PostMapping
    public ResponseEntity<ThemeDto> createTheme(@Valid @RequestBody CreateThemeDto dto) {
        return new ResponseEntity<>(themeService.createTheme(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{themeId}")
    public ResponseEntity<ApiResponse> deleteTheme(@PathVariable Long themeId) {
        final String message = String.format("Success deleting theme with id = %s", themeId);
        themeService.deleteById(themeId);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
