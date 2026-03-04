package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.dto.lesson.LessonDto;

import java.util.List;

public record CourseAdminDetailsDto(
        CourseDto course,
        List<LessonDto> lessons
) {
}
