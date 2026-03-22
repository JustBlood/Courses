package ru.just.monolithmvp.dto.course;

import java.util.List;

public record CourseInNotInListsDto(
        List<CourseDto> in,
        List<CourseDto> notIn
) {
}
