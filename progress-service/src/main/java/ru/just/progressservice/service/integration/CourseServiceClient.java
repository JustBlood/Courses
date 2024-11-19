package ru.just.progressservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.just.progressservice.dto.CourseDto;
import ru.just.progressservice.dto.LessonDto;
import ru.just.progressservice.dto.ModuleDto;
import ru.just.progressservice.dto.ThemeDto;

import java.util.List;

@FeignClient(name = "course-service", path = "/api/v1")
public interface CourseServiceClient {
    @GetMapping("/courses/{courseId}")
    CourseDto getCourseById(@PathVariable Long courseId);

    @GetMapping("/modules/byCourse/{courseId}")
    List<ModuleDto> getModules(@PathVariable Long courseId);

    @GetMapping("themes/byCourse/{courseId}")
    List<ThemeDto> getThemes(@PathVariable Long courseId);

    @GetMapping("/lessons/{lessonId}")
    LessonDto getLessonById(Long lessonId);
}

