package ru.just.courses.controller.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.lesson.CreateLessonDto;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.service.lesson.LessonService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<LessonDto> createLesson(@RequestBody CreateLessonDto createLessonDto) {
        return ResponseEntity.ok(lessonService.createLesson(createLessonDto));
    }

    @GetMapping(value = "/{lessonId}")
    public ResponseEntity<LessonDto> getLessonDtoById(@PathVariable("lessonId") Long lessonId) {
        return ResponseEntity.of(lessonService.getLessonById(lessonId));
    }
}
