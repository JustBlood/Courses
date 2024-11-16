package ru.just.courses.controller.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.just.courses.dto.lesson.CreateLessonDto;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.service.lesson.LessonService;
import ru.just.dtolib.response.ApiResponse;

import java.io.InputStream;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<LessonDto> createLesson(@RequestBody CreateLessonDto createLessonDto) {
        return ResponseEntity.ok(lessonService.createLesson(createLessonDto));
    }

    @PostMapping(value = "/html/{lessonId}/content")
    public ResponseEntity<ApiResponse> loadHtmlContent(@PathVariable("lessonId") Long lessonId,
                                                              @RequestBody InputStream inputStream,
                                                              @RequestHeader(name = HttpHeaders.CONTENT_LENGTH, defaultValue = "-1L") String contentLength) {
        final String message = String.format("Success adding text content to lesson with id = %s", lessonId);
        lessonService.loadHtmlContentToLesson(lessonId, inputStream, Long.parseLong(contentLength));
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping(value = "/html/{lessonId}/content", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> getThemeTextContent(@PathVariable("lessonId") Long lessonId) {
        StreamingResponseBody streamingResponseBody = out -> lessonService.writeTextThemeContentToResponse(lessonId, out);
        return new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
    }

    @GetMapping(value = "/{lessonId}")
    public ResponseEntity<LessonDto> getLessonDtoById(@PathVariable("lessonId") Long lessonId) {
        return ResponseEntity.of(lessonService.getLessonById(lessonId));
    }
}
