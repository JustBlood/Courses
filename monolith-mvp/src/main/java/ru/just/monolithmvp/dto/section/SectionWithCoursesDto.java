package ru.just.monolithmvp.dto.section;

import ru.just.monolithmvp.dto.course.CourseSummaryDto;

import java.util.List;

public record SectionWithCoursesDto(
        Long id,
        String title,
        String description,
        Integer priority,
        List<CourseSummaryDto> courses
) {
}
