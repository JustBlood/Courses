package ru.just.courses.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.just.courses.dto.CreateThemeDto;
import ru.just.courses.dto.ThemeDto;
import ru.just.courses.service.ThemeService;
import ru.just.dtolib.response.ApiResponse;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/themes")
public class ThemeController {
    private final ThemeService themeService;
    private final ExecutorService asyncStreamingDataExecutor = Executors.newCachedThreadPool();

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

    @PostMapping(value = "/content/text/{themeId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ApiResponse> createTextThemeContent(@PathVariable("themeId") Long themeId,
                                                              @RequestParam(name = "charactersLength") Integer textLength,
                                                              HttpServletRequest httpServletRequest) throws IOException {
        final String message = String.format("Success adding text content to theme with id = %s", themeId);
        themeService.addTextContentToTheme(themeId, httpServletRequest.getInputStream(), textLength);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping(value = "/content/text/{themeId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> getThemeTextContent(@PathVariable("themeId") Long themeId) {
        StreamingResponseBody streamingResponseBody = out -> {
            themeService.writeTextThemeContentToResponse(themeId, out);
        };
        return new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
    }
}
