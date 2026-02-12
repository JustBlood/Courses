package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.model.Course;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseDto toDto(Course course);
}
